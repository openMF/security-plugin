/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.fineract.selfservice.registration.service;

import org.apache.fineract.infrastructure.campaigns.sms.service.SmsCampaignDropdownReadPlatformService;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.SelfServicePluginEmailService;
import org.apache.fineract.infrastructure.security.service.PlatformPasswordEncoder;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageRepository;
import org.apache.fineract.infrastructure.sms.scheduler.SmsMessageScheduledJobService;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.selfservice.registration.domain.SelfServiceRegistration;
import org.apache.fineract.selfservice.registration.domain.SelfServiceRegistrationRepository;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUserClientMappingRepository;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUserRepository;
import org.apache.fineract.selfservice.useradministration.domain.SelfServiceUserDomainService;
import org.apache.fineract.selfservice.useradministration.service.AppSelfServiceUserReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.domain.PasswordValidationPolicyRepository;
import org.apache.fineract.useradministration.domain.RoleRepository;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thymeleaf.ITemplateEngine;

public class SelfServiceForgotPasswordWritePlatformServiceImpl
        implements SelfServiceForgotPassworWritePlatformService {

    private final SelfServiceRegistrationRepository selfServiceRegistrationRepository;
    private final FromJsonHelper fromApiJsonHelper;
    private final SelfServiceRegistrationReadPlatformService selfServiceRegistrationReadPlatformService;
    private final ClientRepositoryWrapper clientRepository;
    private final PasswordValidationPolicyRepository passwordValidationPolicy;
    private final SelfServiceUserDomainService userDomainService;
    private final SelfServicePluginEmailService selfServicePluginEmailService;
    private final SmsMessageRepository smsMessageRepository;
    private final SmsMessageScheduledJobService smsMessageScheduledJobService;
    private final SmsCampaignDropdownReadPlatformService smsCampaignDropdownReadPlatformService;
    private final AppSelfServiceUserReadPlatformService appUserReadPlatformService;
    private final RoleRepository roleRepository;
    private final AppSelfServiceUserClientMappingRepository appUserClientMappingRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AppUserRepository appUserRepository;
    private final Environment env;
    private final PlatformPasswordEncoder platformPasswordEncoder;
    private final AppSelfServiceUserRepository appSelfServiceUserRepository;
    private final SelfServiceAuthorizationTokenService selfServiceAuthorizationTokenService;
    private final ITemplateEngine registrationTemplateEngine;
    private final MessageSource registrationMessageSource;

    public SelfServiceForgotPasswordWritePlatformServiceImpl(
            SelfServiceRegistrationRepository selfServiceRegistrationRepository,
            FromJsonHelper fromApiJsonHelper,
            SelfServiceRegistrationReadPlatformService selfServiceRegistrationReadPlatformService,
            ClientRepositoryWrapper clientRepository,
            PasswordValidationPolicyRepository passwordValidationPolicy,
            SelfServiceUserDomainService userDomainService,
            SelfServicePluginEmailService selfServicePluginEmailService,
            SmsMessageRepository smsMessageRepository,
            SmsMessageScheduledJobService smsMessageScheduledJobService,
            SmsCampaignDropdownReadPlatformService smsCampaignDropdownReadPlatformService,
            AppSelfServiceUserReadPlatformService appUserReadPlatformService,
            RoleRepository roleRepository,
            AppSelfServiceUserClientMappingRepository appUserClientMappingRepository,
            JdbcTemplate jdbcTemplate,
            AppUserRepository appUserRepository,
            Environment env,
            PlatformPasswordEncoder platformPasswordEncoder,
            AppSelfServiceUserRepository appSelfServiceUserRepository,
            SelfServiceAuthorizationTokenService selfServiceAuthorizationTokenService,
            ITemplateEngine registrationTemplateEngine,
            MessageSource registrationMessageSource) {
        this.selfServiceRegistrationRepository = selfServiceRegistrationRepository;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.selfServiceRegistrationReadPlatformService = selfServiceRegistrationReadPlatformService;
        this.clientRepository = clientRepository;
        this.passwordValidationPolicy = passwordValidationPolicy;
        this.userDomainService = userDomainService;
        this.selfServicePluginEmailService = selfServicePluginEmailService;
        this.smsMessageRepository = smsMessageRepository;
        this.smsMessageScheduledJobService = smsMessageScheduledJobService;
        this.smsCampaignDropdownReadPlatformService = smsCampaignDropdownReadPlatformService;
        this.appUserReadPlatformService = appUserReadPlatformService;
        this.roleRepository = roleRepository;
        this.appUserClientMappingRepository = appUserClientMappingRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.appUserRepository = appUserRepository;
        this.env = env;
        this.platformPasswordEncoder = platformPasswordEncoder;
        this.appSelfServiceUserRepository = appSelfServiceUserRepository;
        this.selfServiceAuthorizationTokenService = selfServiceAuthorizationTokenService;
        this.registrationTemplateEngine = registrationTemplateEngine;
        this.registrationMessageSource = registrationMessageSource;
    }

    @Override
    public SelfServiceRegistration requestPasswordReset(String apiRequestBodyAsJson) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void resetPassword(String apiRequestBodyAsJson) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
