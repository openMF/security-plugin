package org.apache.fineract.infrastructure.security.service;

import static org.springframework.security.authorization.AuthenticatedAuthorizationManager.fullyAuthenticated;
import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasAuthority;
import static org.springframework.security.authorization.AuthorizationManagers.allOf;

import java.util.ArrayList;
import java.util.List;

import org.apache.fineract.infrastructure.businessdate.service.BusinessDateReadPlatformService;
import org.apache.fineract.infrastructure.cache.service.CacheWritePlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.apache.fineract.infrastructure.core.domain.FineractRequestContextHolder;
import org.apache.fineract.infrastructure.core.filters.CallerIpTrackingFilter;
import org.apache.fineract.infrastructure.core.filters.CorrelationHeaderFilter;
import org.apache.fineract.infrastructure.core.filters.IdempotencyStoreFilter;
import org.apache.fineract.infrastructure.core.filters.IdempotencyStoreHelper;
import org.apache.fineract.infrastructure.core.filters.RequestResponseFilter;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.MDCWrapper;
import org.apache.fineract.infrastructure.instancemode.filter.FineractInstanceModeApiFilter;
import org.apache.fineract.infrastructure.jobs.filter.LoanCOBApiFilter;
import org.apache.fineract.infrastructure.jobs.filter.LoanCOBFilterHelper;
import org.apache.fineract.infrastructure.jobs.filter.ProgressiveLoanModelCheckerFilter;
import org.apache.fineract.infrastructure.security.data.PlatformRequestLog;
import org.apache.fineract.infrastructure.security.filter.TenantAwareBasicAuthenticationFilter;
import org.apache.fineract.infrastructure.security.filter.TwoFactorAuthenticationFilter;
import static org.apache.fineract.infrastructure.security.service.SelfServiceUserAuthorizationManager.selfServiceUserAuthManager;
import org.apache.fineract.notification.service.UserNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@ConditionalOnProperty("fineract.security.basicauth.enabled")
@EnableMethodSecurity
@Order(1) // Ensures this is checked BEFORE the main API security chain
public class SelfServiceSecurityConfiguration {

    private static final PathPatternRequestMatcher.Builder API_MATCHER = PathPatternRequestMatcher.withDefaults();

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private TenantAwareJpaPlatformUserDetailsService userDetailsService;
    @Autowired
    private FineractProperties fineractProperties;
    @Autowired
    private ServerProperties serverProperties;
    @Autowired
    private ToApiJsonSerializer<PlatformRequestLog> toApiJsonSerializer;
    @Autowired
    private ConfigurationDomainService configurationDomainService;
    @Autowired
    private CacheWritePlatformService cacheWritePlatformService;
    @Autowired
    private UserNotificationService userNotificationService;
    @Autowired
    private AuthTenantDetailsService basicAuthTenantDetailsService;
    @Autowired
    private BusinessDateReadPlatformService businessDateReadPlatformService;
    @Autowired
    private MDCWrapper mdcWrapper;
    @Autowired
    private FineractRequestContextHolder fineractRequestContextHolder;
    @Autowired(required = false)
    private LoanCOBFilterHelper loanCOBFilterHelper;
    @Autowired
    private IdempotencyStoreHelper idempotencyStoreHelper;
    @Autowired
    private ProgressiveLoanModelCheckerFilter progressiveLoanModelCheckerFilter;
    @Autowired
    private PlatformUserDetailsChecker platformUserDetailsChecker;

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // CRITICAL: Scope this filter chain ONLY to /api/v1/self/**
        // This prevents it from conflicting with the main API endpoints
        http.securityMatcher(API_MATCHER.matcher("/api/v1/self/**"))
            .authorizeHttpRequests(auth -> {

                List<AuthorizationManager<RequestAuthorizationContext>> authorizationManagers = new ArrayList<>();
                authorizationManagers.add(fullyAuthenticated());

                // 1. Public Endpoints (Authentication & Registration)
                auth.requestMatchers(API_MATCHER.matcher(HttpMethod.POST, "/api/*/self/authentication")).permitAll()
                        .requestMatchers(API_MATCHER.matcher(HttpMethod.POST, "/api/*/self/registration")).permitAll()
                        .requestMatchers(API_MATCHER.matcher(HttpMethod.POST, "/api/*/self/registration/user")).permitAll()
                        .requestMatchers(API_MATCHER.matcher(HttpMethod.OPTIONS, "/api/**")).permitAll();

                // 2. Two-Factor Authentication endpoints (if enabled)
                if (fineractProperties.getSecurity().getTwoFactor().isEnabled()) {
                    auth.requestMatchers(API_MATCHER.matcher(HttpMethod.POST, "/api/*/self/twofactor/validate")).permitAll();
                    authorizationManagers.add(hasAuthority("TWOFACTOR_AUTHENTICATED"));
                }

                // 3. Add Self-Service specific authorization manager
                // This ensures the user is authenticated AND has the correct self-service permissions
                authorizationManagers.add(selfServiceUserAuthManager());

                // 4. Apply the authorization managers to all other /api/v1/self/** requests
                auth.requestMatchers(API_MATCHER.matcher("/api/v1/self/**"))
                        .access(allOf(authorizationManagers.toArray(new AuthorizationManager[0])));

            }).httpBasic(hb -> hb.authenticationEntryPoint(basicAuthenticationEntryPoint()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(smc -> smc.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(tenantAwareBasicAuthenticationFilter(), SecurityContextHolderFilter.class)
            .addFilterAfter(requestResponseFilter(), ExceptionTranslationFilter.class)
            .addFilterAfter(correlationHeaderFilter(), RequestResponseFilter.class)
            .addFilterAfter(fineractInstanceModeApiFilter(), CorrelationHeaderFilter.class);

        if (loanCOBFilterHelper != null) {
            http.addFilterAfter(loanCOBApiFilter(), FineractInstanceModeApiFilter.class).addFilterAfter(idempotencyStoreFilter(),
                    LoanCOBApiFilter.class);
            http.addFilterBefore(progressiveLoanModelCheckerFilter, LoanCOBApiFilter.class);
        } else {
            http.addFilterAfter(idempotencyStoreFilter(), FineractInstanceModeApiFilter.class);
            http.addFilterAfter(progressiveLoanModelCheckerFilter, FineractInstanceModeApiFilter.class);
        }

        if (fineractProperties.getIpTracking().isEnabled()) {
            http.addFilterAfter(callerIpTrackingFilter(), RequestResponseFilter.class);
        }

        if (fineractProperties.getSecurity().getTwoFactor().isEnabled()) {
            http.addFilterAfter(twoFactorAuthenticationFilter(), CorrelationHeaderFilter.class);
        }

        if (serverProperties.getSsl().isEnabled()) {
            http.requiresChannel(channel -> channel.requestMatchers(API_MATCHER.matcher("/api/**")).requiresSecure());
        }

        if (fineractProperties.getSecurity().getHsts().isEnabled()) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure()).headers(
                    headers -> headers.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)));
        }

        if (fineractProperties.getSecurity().getCors().isEnabled()) {
            http.cors(Customizer.withDefaults());
        }

        return http.build();
    }

    public RequestResponseFilter requestResponseFilter() {
        return new RequestResponseFilter();
    }

    public LoanCOBApiFilter loanCOBApiFilter() {
        return new LoanCOBApiFilter(loanCOBFilterHelper);
    }

    public TwoFactorAuthenticationFilter twoFactorAuthenticationFilter() {
        TwoFactorService twoFactorService = applicationContext.getBean(TwoFactorService.class);
        return new TwoFactorAuthenticationFilter(twoFactorService);
    }

    public FineractInstanceModeApiFilter fineractInstanceModeApiFilter() {
        return new FineractInstanceModeApiFilter(fineractProperties);
    }

    public IdempotencyStoreFilter idempotencyStoreFilter() {
        return new IdempotencyStoreFilter(fineractRequestContextHolder, idempotencyStoreHelper, fineractProperties);
    }

    public CorrelationHeaderFilter correlationHeaderFilter() {
        return new CorrelationHeaderFilter(fineractProperties, mdcWrapper);
    }

    public CallerIpTrackingFilter callerIpTrackingFilter() {
        return new CallerIpTrackingFilter(fineractProperties);
    }

    public TenantAwareBasicAuthenticationFilter tenantAwareBasicAuthenticationFilter() throws Exception {
        TenantAwareBasicAuthenticationFilter filter = new TenantAwareBasicAuthenticationFilter(authenticationManagerBean(),
                basicAuthenticationEntryPoint(), toApiJsonSerializer, configurationDomainService, cacheWritePlatformService,
                userNotificationService, basicAuthTenantDetailsService, businessDateReadPlatformService);

        // Scope the auth filter to /api/** so it can pick up the tenant header
        filter.setRequestMatcher(API_MATCHER.matcher("/api/**"));
        return filter;
    }

    @Bean
    public BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
        BasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new BasicAuthenticationEntryPoint();
        basicAuthenticationEntryPoint.setRealmName("Fineract Platform API");
        return basicAuthenticationEntryPoint;
    }

    @Bean(name = "customAuthenticationProvider")
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setPostAuthenticationChecks(platformUserDetailsChecker);
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        ProviderManager providerManager = new ProviderManager(authProvider());
        providerManager.setEraseCredentialsAfterAuthentication(false);
        return providerManager;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        FineractProperties.CorsProperties corsConfiguration = fineractProperties.getSecurity().getCors();
        config.setAllowedOriginPatterns(corsConfiguration.getAllowedOriginPatterns());
        config.setAllowedMethods(corsConfiguration.getAllowedMethods());
        config.setAllowedHeaders(corsConfiguration.getAllowedHeaders());
        config.setExposedHeaders(corsConfiguration.getExposedHeaders());
        config.setAllowCredentials(corsConfiguration.isAllowCredentials());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}