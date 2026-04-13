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
package org.apache.fineract.selfservice.registration.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.campaigns.sms.data.SmsProviderData;
import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsCampaign;
import org.apache.fineract.infrastructure.campaigns.sms.service.SmsCampaignDropdownReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.domain.EmailDetail;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.GmailBackedPlatformEmailService;
import org.apache.fineract.infrastructure.sms.domain.SmsMessage;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageRepository;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageStatusType;
import org.apache.fineract.infrastructure.sms.scheduler.SmsMessageScheduledJobService;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.selfservice.registration.SelfServiceApiConstants;
import org.apache.fineract.selfservice.registration.domain.SelfServiceRegistration;
import org.apache.fineract.selfservice.registration.domain.SelfServiceRegistrationRepository;
import org.apache.fineract.selfservice.registration.exception.SelfServiceEnrollmentConflictException;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUserClientMappingRepository;
import org.apache.fineract.useradministration.domain.PasswordValidationPolicyRepository;
import org.apache.fineract.useradministration.domain.RoleRepository;
import org.apache.fineract.selfservice.useradministration.domain.SelfServiceUserDomainService;
import org.apache.fineract.selfservice.useradministration.service.AppSelfServiceUserReadPlatformService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.portfolio.client.service.ClientWritePlatformService;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUserClientMapping;

@Slf4j
@RequiredArgsConstructor
public class SelfServiceForgotPasswordWritePlatformServiceImpl implements SelfServiceForgotPassworWritePlatformService {

    private final SelfServiceRegistrationRepository selfServiceRegistrationRepository;
    private final FromJsonHelper fromApiJsonHelper;
    private final SelfServiceRegistrationReadPlatformService selfServiceRegistrationReadPlatformService;
    private final ClientRepositoryWrapper clientRepository;
    private final PasswordValidationPolicyRepository passwordValidationPolicy;
    private final SelfServiceUserDomainService userDomainService;
    private final GmailBackedPlatformEmailService gmailBackedPlatformEmailService;
    private final SmsMessageRepository smsMessageRepository;
    private final SmsMessageScheduledJobService smsMessageScheduledJobService;
    private final SmsCampaignDropdownReadPlatformService smsCampaignDropdownReadPlatformService;
    private final AppSelfServiceUserReadPlatformService appUserReadPlatformService;
    private final RoleRepository roleRepository;
    private static final SecureRandom secureRandom = new SecureRandom();
    private final AppSelfServiceUserClientMappingRepository appUserClientMappingRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AppUserRepository appUserRepository;
    private final ClientWritePlatformService clientWritePlatformService;
    private final Environment env;

    @Override
    public SelfServiceRegistration createForgotPasswordRequest(String apiRequestBodyAsJson) {
        Gson gson = new Gson();
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("user");
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, apiRequestBodyAsJson,
                SelfServiceApiConstants.FORGOT_PASSWORD_REQUEST_DATA_PARAMETERS);
        JsonElement element = gson.fromJson(apiRequestBodyAsJson.toString(), JsonElement.class);

        String userName = this.fromApiJsonHelper.extractStringNamed(SelfServiceApiConstants.usernameParamName, element);
        baseDataValidator.reset().parameter(SelfServiceApiConstants.usernameParamName).value(userName).notBlank().notExceedingLengthOf(100);
        
        /*TODO: If present the externalId of the client can be used for comparing if it matches the records
        String externalId = this.fromApiJsonHelper.extractStringNamed(SelfServiceApiConstants.externalIdParamName, element);
        baseDataValidator.reset().parameter(SelfServiceApiConstants.externalIdParamName).value(externalId).notNull().notBlank()
                .notExceedingLengthOf(100);
        */

        String authenticationMode = this.fromApiJsonHelper.extractStringNamed(SelfServiceApiConstants.authenticationModeParamName, element);
        baseDataValidator.reset().parameter(SelfServiceApiConstants.authenticationModeParamName).value(authenticationMode).notBlank();
        
        boolean isEmailAuthenticationMode = authenticationMode.equalsIgnoreCase(SelfServiceApiConstants.emailModeParamName);
        
        validateForExistingUsername(userName);

        throwExceptionIfValidationError(dataValidationErrors, userName, authenticationMode);

        String authenticationToken = randomAuthorizationTokenGeneration();
        
        AppSelfServiceUserClientMapping appSelfServiceUserClientMapping = this.appUserClientMappingRepository.fetchByAppuserUsername(userName);
        
        Client client = this.clientRepository.getClientByAccountNumber(appSelfServiceUserClientMapping.getClient().getAccountNumber());
        
        SelfServiceRegistration selfServiceRegistration = SelfServiceRegistration.instance(client, "", "", "", "",
                "", appSelfServiceUserClientMapping.getAppUser().getEmail(), authenticationToken, userName, "");
        this.selfServiceRegistrationRepository.saveAndFlush(selfServiceRegistration);
        sendAuthorizationToken(selfServiceRegistration, isEmailAuthenticationMode);
        return selfServiceRegistration;

    }

    public void validateForExistingUsername(String username) {
        boolean isExistingUserName = this.appUserReadPlatformService.isUsernameExist(username);
        if (!isExistingUserName) {
            final StringBuilder defaultMessageBuilder = new StringBuilder("User with username ").append(username)
                    .append(" not found.");
            throw new PlatformDataIntegrityException("error.msg.user.notfound.username", defaultMessageBuilder.toString(),
                    SelfServiceApiConstants.usernameParamName, username);
        }
    }

    public void sendAuthorizationToken(SelfServiceRegistration selfServiceRegistration, Boolean isEmailAuthenticationMode) {
        if (isEmailAuthenticationMode) {
            sendAuthorizationMail(selfServiceRegistration);
        } else {
            sendAuthorizationMessage(selfServiceRegistration);
        }
    }

    private void sendAuthorizationMessage(SelfServiceRegistration selfServiceRegistration) {
        Collection<SmsProviderData> smsProviders = this.smsCampaignDropdownReadPlatformService.retrieveSmsProviders();
        if (smsProviders.isEmpty()) {
            throw new PlatformDataIntegrityException("error.msg.mobile.service.provider.not.available",
                    "Mobile service provider not available.");
        }
        Long providerId = new ArrayList<>(smsProviders).get(0).getId();
        final String message = "Hola  " + selfServiceRegistration.getFirstName() + "," + "\n\n"
                + "Para crear un usuario, utilice los siguientes datos \n" + "\nId de Petición : " + selfServiceRegistration.getId()
                + "\n Código de Autorización : " + selfServiceRegistration.getAuthenticationToken();
        String externalId = null;
        Group group = null;
        Staff staff = null;
        SmsCampaign smsCampaign = null;
        boolean isNotification = false;
        SmsMessage smsMessage = SmsMessage.instance(externalId, group, selfServiceRegistration.getClient(), staff,
                SmsMessageStatusType.PENDING, message, selfServiceRegistration.getMobileNumber(), smsCampaign, isNotification);
        this.smsMessageRepository.save(smsMessage);
        this.smsMessageScheduledJobService.sendTriggeredMessage(new ArrayList<>(Arrays.asList(smsMessage)), providerId);
    }

    private void sendAuthorizationMail(SelfServiceRegistration selfServiceRegistration) {
        final String subject = "Código de Autorización ";
        final String body = "Hola  " + selfServiceRegistration.getFirstName() + "," + "\n" + "Para crear un usuario, utilice los siguientes datos\n\n"
                + "\nId de Petición: " + selfServiceRegistration.getId() + "\nCódigo de Autorización : "
                + selfServiceRegistration.getAuthenticationToken();

        final EmailDetail emailDetail = new EmailDetail(subject, body, selfServiceRegistration.getEmail(),
                selfServiceRegistration.getFirstName());
        this.gmailBackedPlatformEmailService.sendDefinedEmail(emailDetail);
    }

    private void throwExceptionIfValidationError(final List<ApiParameterError> dataValidationErrors, String userName, String authenticationMode) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }        
    }

    public static String randomAuthorizationTokenGeneration() {
        Integer randomPIN = (int) (secureRandom.nextDouble() * 9000) + 1000;
        return randomPIN.toString();
    }


}
