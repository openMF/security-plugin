/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.security.service;

import java.util.List;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Order(1)  // Very important: Must have higher priority than main security config
public class SelfServiceSecurityConfiguration {

    @Autowired
    private FineractProperties fineractProperties;

    @Bean
    public SecurityFilterChain selfServiceSecurityFilterChain(HttpSecurity http) throws Exception {

        http
            // Apply only to self-service endpoints
            .securityMatcher("/api/v1/self/**", "/v1/self/**", "**/v1/self/**")
            
            // Disable CSRF for public self-service APIs
            .csrf(AbstractHttpConfigurer::disable)
            
            // Stateless session
            .sessionManagement(smc -> smc.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // === PUBLIC ENDPOINTS ===
                .requestMatchers(HttpMethod.POST, "/api/v1/self/registration").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/self/registration/user").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/self/registration").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/self/registration/user").permitAll()

                // Self authentication (login)
                .requestMatchers(HttpMethod.POST, "/api/v1/self/authentication").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/self/authentication").permitAll()

                // All other self-service endpoints require self-service authentication
                .requestMatchers("/api/v1/self/**", "/v1/self/**", "**/v1/self/**").authenticated()

                .anyRequest().permitAll()
            );

        // Optional: CORS if needed for mobile/web clients
        if (fineractProperties.getSecurity().getCors().isEnabled()) {
            http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        }

        return http.build();
    }

    // Optional CORS bean (you can keep or remove)
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));   // Change in production
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}