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
import jakarta.persistence.PersistenceException;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.selfservice.registration.SelfServiceApiConstants;
import org.apache.fineract.selfservice.registration.domain.SelfServiceRegistration;
import org.apache.fineract.selfservice.registration.domain.SelfServiceRegistrationRepository;
import org.apache.fineract.selfservice.registration.exception.SelfServiceRegistrationNotFoundException;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUser;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUserClientMappingRepository;
import org.apache.fineract.useradministration.domain.PasswordValidationPolicy;
import org.apache.fineract.useradministration.domain.PasswordValidationPolicyRepository;
import org.apache.fineract.useradministration.domain.Role;
import org.apache.fineract.useradministration.domain.RoleRepository;
import org.apache.fineract.selfservice.useradministration.domain.SelfServiceUserDomainService;
import org.apache.fineract.useradministration.exception.RoleNotFoundException;
import org.apache.fineract.selfservice.useradministration.service.AppSelfServiceUserReadPlatformService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SelfServiceRegistrationWritePlatformServiceImpl implements SelfServiceRegistrationWritePlatformService {

    public record RegistrationContext(
            SelfServiceRegistrationRepository selfServiceRegistrationRepository,
            FromJsonHelper fromApiJsonHelper,
            SelfServiceRegistrationReadPlatformService selfServiceRegistrationReadPlatformService,
            ClientRepositoryWrapper clientRepository,
            PasswordValidationPolicyRepository passwordValidationPolicy,
            SelfServiceUserDomainService userDomainService,
            AppSelfServiceUserReadPlatformService appUserReadPlatformService,
            RoleRepository roleRepository,
            AppSelfServiceUserClientMappingRepository appUserClientMappingRepository
    ) {}

    public record NotificationContext(
            GmailBackedPlatformEmailService gmailBackedPlatformEmailService,
            SmsMessageRepository smsMessageRepository,
            SmsMessageScheduledJobService smsMessageScheduledJobService,
            SmsCampaignDropdownReadPlatformService smsCampaignDropdownReadPlatformService
    ) {}

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

    public SelfServiceRegistrationWritePlatformServiceImpl(RegistrationContext regCtx, NotificationContext notifCtx) {
        this.selfServiceRegistrationRepository = regCtx.selfServiceRegistrationRepository();
        this.fromApiJsonHelper = regCtx.fromApiJsonHelper();
        this.selfServiceRegistrationReadPlatformService = regCtx.selfServiceRegistrationReadPlatformService();
        this.clientRepository = regCtx.clientRepository();
        this.passwordValidationPolicy = regCtx.passwordValidationPolicy();
        this.userDomainService = regCtx.userDomainService();
        this.appUserReadPlatformService = regCtx.appUserReadPlatformService();
        this.roleRepository = regCtx.roleRepository();
        this.appUserClientMappingRepository = regCtx.appUserClientMappingRepository();

        this.gmailBackedPlatformEmailService = notifCtx.gmailBackedPlatformEmailService();
        this.smsMessageRepository = notifCtx.smsMessageRepository();
        this.smsMessageScheduledJobService = notifCtx.smsMessageScheduledJobService();
        this.smsCampaignDropdownReadPlatformService = notifCtx.smsCampaignDropdownReadPlatformService();
    }

    private record RegistrationPayload(String accountNumber, String firstName, String middleName, String lastName, String username, String email, String mobileNumber, String authenticationMode) {}

    @Override
    @Transactional
    public SelfServiceRegistration createRegistrationRequest(String apiRequestBodyAsJson) {
        RegistrationPayload payload = parseAndValidate(apiRequestBodyAsJson);

        String authenticationToken = randomAuthorizationTokenGeneration();
        Client client = this.clientRepository.getClientByAccountNumber(payload.accountNumber());
        SelfServiceRegistration selfServiceRegistration = this.selfServiceRegistrationRepository.saveAndFlush(
                SelfServiceRegistration.instance(client.getId(), payload.accountNumber(), payload.firstName(), payload.middleName(), payload.lastName(),
                        payload.mobileNumber(), payload.email(), authenticationToken, payload.username(),
                        org.apache.fineract.infrastructure.core.service.DateUtils.getLocalDateTimeOfSystem()));
        boolean isEmailAuth = payload.authenticationMode().equalsIgnoreCase(SelfServiceApiConstants.emailModeParamName);
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendAuthorizationToken(selfServiceRegistration, client, isEmailAuth);
                }
            });
        return selfServiceRegistration;
    }

    private RegistrationPayload parseAndValidate(String apiRequestBodyAsJson) {
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, apiRequestBodyAsJson, SelfServiceApiConstants.REGISTRATION_REQUEST_DATA_PARAMETERS);
        JsonElement element = new Gson().fromJson(apiRequestBodyAsJson, JsonElement.class);

        RegistrationPayload payload = extractPayload(element);
        validatePayload(payload);
        return payload;
    }

    private RegistrationPayload extractPayload(JsonElement element) {
        String authMode = this.fromApiJsonHelper.extractStringNamed(SelfServiceApiConstants.authenticationModeParamName, element);
        boolean isEmailAuth = authMode != null && authMode.equalsIgnoreCase(SelfServiceApiConstants.emailModeParamName);
        return new RegistrationPayload(
            this.fromApiJsonHelper.extractStringNamed(SelfServiceApiConstants.accountNumberParamName, element),
            this.fromApiJsonHelper.extractStringNamed(SelfServiceApiConstants.firstNameParamName, element),
            this.fromApiJsonHelper.extractStringNamed(SelfServiceApiConstants.middleNameParamName, element),
            this.fromApiJsonHelper.extractStringNamed(SelfServiceApiConstants.lastNameParamName, element),
            this.fromApiJsonHelper.extractStringNamed(SelfServiceApiConstants.usernameParamName, element),
            this.fromApiJsonHelper.extractStringNamed(SelfServiceApiConstants.emailParamName, element),
            !isEmailAuth ? this.fromApiJsonHelper.extractStringNamed(SelfServiceApiConstants.mobileNumberParamName, element) : null,
            authMode
        );
    }

    private void validatePayload(RegistrationPayload payload) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("user");

        baseDataValidator.reset().parameter(SelfServiceApiConstants.accountNumberParamName).value(payload.accountNumber()).notNull().notBlank().notExceedingLengthOf(100);
        baseDataValidator.reset().parameter(SelfServiceApiConstants.firstNameParamName).value(payload.firstName()).notBlank().notExceedingLengthOf(100);
        baseDataValidator.reset().parameter(SelfServiceApiConstants.middleNameParamName).value(payload.middleName()).ignoreIfNull().notExceedingLengthOf(100);
        baseDataValidator.reset().parameter(SelfServiceApiConstants.lastNameParamName).value(payload.lastName()).notBlank().notExceedingLengthOf(100);
        baseDataValidator.reset().parameter(SelfServiceApiConstants.usernameParamName).value(payload.username()).notBlank().notExceedingLengthOf(100);

        baseDataValidator.reset().parameter(SelfServiceApiConstants.authenticationModeParamName).value(payload.authenticationMode()).notBlank()
                .isOneOfTheseStringValues(SelfServiceApiConstants.emailModeParamName, SelfServiceApiConstants.mobileModeParamName);
        baseDataValidator.reset().parameter(SelfServiceApiConstants.emailParamName).value(payload.email()).notNull().notBlank().notExceedingLengthOf(100);

        boolean isEmailAuth = payload.authenticationMode() != null && payload.authenticationMode().equalsIgnoreCase(SelfServiceApiConstants.emailModeParamName);
        if (!isEmailAuth) {
            baseDataValidator.reset().parameter(SelfServiceApiConstants.mobileNumberParamName).value(payload.mobileNumber()).notNull().validatePhoneNumber();
        }
        
        validateForDuplicateUsername(payload.username());
        throwExceptionIfValidationError(dataValidationErrors, payload.accountNumber(), payload.firstName(), payload.middleName(), payload.lastName(), payload.mobileNumber(), isEmailAuth);
    }

    public void validateForDuplicateUsername(String username) {
        boolean isDuplicateUserName = this.appUserReadPlatformService.isUsernameExist(username);
        if (isDuplicateUserName) {
            throw new PlatformDataIntegrityException("error.msg.user.duplicate.username", 
                    "User with username " + username + " already exists.", SelfServiceApiConstants.usernameParamName, username);
        }
    }

    public void sendAuthorizationToken(SelfServiceRegistration selfServiceRegistration, Client client, Boolean isEmailAuthenticationMode) {
        if (isEmailAuthenticationMode) {
            sendAuthorizationMail(selfServiceRegistration);
        } else {
            sendAuthorizationMessage(selfServiceRegistration, client);
        }
    }

    private void sendAuthorizationMessage(SelfServiceRegistration selfServiceRegistration, Client client) {
        Collection<SmsProviderData> smsProviders = this.smsCampaignDropdownReadPlatformService.retrieveSmsProviders();
        if (smsProviders.isEmpty()) {
            throw new PlatformDataIntegrityException("error.msg.mobile.service.provider.not.available", "Mobile service provider not available.");
        }
        Long providerId = new ArrayList<>(smsProviders).get(0).getId();
        final String message = String.format("Hola %s,\n\nPara crear un usuario, utilice los siguientes datos\n\nId de Petición : %s\nCódigo de Autorización : %s",
                selfServiceRegistration.getFirstName(), selfServiceRegistration.getId(), selfServiceRegistration.getAuthenticationToken());
        
        SmsMessage smsMessage = java.util.Objects.requireNonNull(
                SmsMessage.instance(null, null, client, null,
                        SmsMessageStatusType.PENDING, message, selfServiceRegistration.getMobileNumber(), null, false));
        this.smsMessageRepository.save(smsMessage);
        this.smsMessageScheduledJobService.sendTriggeredMessage(new ArrayList<>(Arrays.asList(smsMessage)), providerId);
    }

    private void sendAuthorizationMail(SelfServiceRegistration selfServiceRegistration) {
        final String subject = "Código de Autorización ";
        final String body = String.format("Hola %s,\nPara crear un usuario, utilice los siguientes datos\n\nId de Petición: %s\nCódigo de Autorización : %s",
                selfServiceRegistration.getFirstName(), selfServiceRegistration.getId(), selfServiceRegistration.getAuthenticationToken());

        final EmailDetail emailDetail = new EmailDetail(subject, body, selfServiceRegistration.getEmail(), selfServiceRegistration.getFirstName());
        this.gmailBackedPlatformEmailService.sendDefinedEmail(emailDetail);
    }

    private void throwExceptionIfValidationError(final List<ApiParameterError> dataValidationErrors, String accountNumber, String firstName,
            String middleName, String lastName, String mobileNumber, boolean isEmailAuthenticationMode) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
        boolean isClientExist = this.selfServiceRegistrationReadPlatformService.isClientExist(accountNumber, firstName, middleName, lastName,
                mobileNumber, isEmailAuthenticationMode);
        if (!isClientExist) {
            throw new ClientNotFoundException();
        }
    }

    public static String randomAuthorizationTokenGeneration() {
        Integer randomPIN = (int) (secureRandom.nextDouble() * 9000) + 1000;
        return randomPIN.toString();
    }

    @Override
    @Transactional
    public AppSelfServiceUser createSelfServiceUser(String apiRequestBodyAsJson) {
        JsonCommand command = null;
        String username = null;
        try {
            JsonElement element = validateAndParseUserRequest(apiRequestBodyAsJson);
            Long id = this.fromApiJsonHelper.extractLongNamed(SelfServiceApiConstants.requestIdParamName, element);
            String token = this.fromApiJsonHelper.extractStringNamed(SelfServiceApiConstants.authenticationTokenParamName, element);
            String password = this.fromApiJsonHelper.extractStringNamed(SelfServiceApiConstants.passwordParamName, element);
            command = JsonCommand.fromJsonElement(id, element);

            SelfServiceRegistration reg = this.selfServiceRegistrationRepository.getRequestByIdAndAuthenticationToken(id, token);
            if (reg == null) {
                throw new SelfServiceRegistrationNotFoundException(id, token);
            }
            username = reg.getUsername();
            return doCreateSelfServiceUser(reg, password);
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            throw handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve, username);
        } catch (final PersistenceException | AuthenticationServiceException dve) {
            throw handleDataIntegrityIssues(command, ExceptionUtils.getRootCause(dve.getCause()), dve, username);
        }
    }

    private JsonElement validateAndParseUserRequest(String apiRequestBodyAsJson) {
        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, apiRequestBodyAsJson, SelfServiceApiConstants.CREATE_USER_REQUEST_DATA_PARAMETERS);
        JsonElement element = new Gson().fromJson(apiRequestBodyAsJson, JsonElement.class);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("user");

        Long id = this.fromApiJsonHelper.extractLongNamed(SelfServiceApiConstants.requestIdParamName, element);
        baseDataValidator.reset().parameter(SelfServiceApiConstants.requestIdParamName).value(id).notNull().integerGreaterThanZero();
        String authenticationToken = this.fromApiJsonHelper.extractStringNamed(SelfServiceApiConstants.authenticationTokenParamName, element);
        baseDataValidator.reset().parameter(SelfServiceApiConstants.authenticationTokenParamName).value(authenticationToken).notBlank()
                .notNull().notExceedingLengthOf(100);

        String password = this.fromApiJsonHelper.extractStringNamed(SelfServiceApiConstants.passwordParamName, element);
        final PasswordValidationPolicy validationPolicy = this.passwordValidationPolicy.findActivePasswordValidationPolicy();
        baseDataValidator.reset().parameter(SelfServiceApiConstants.passwordParamName).value(password)
                .notNull()
                .matchesRegularExpression(validationPolicy.getRegex(), validationPolicy.getDescription()).notExceedingLengthOf(100);

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
        return element;
    }

    private AppSelfServiceUser doCreateSelfServiceUser(SelfServiceRegistration reg, String password) {
        Client client = this.clientRepository.findOneWithNotFoundDetection(reg.getClientId());
        final boolean passwordNeverExpire = true;
        final boolean isSelfServiceUser = true;
        final Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("DUMMY_ROLE_NOT_USED_OR_PERSISTED_TO_AVOID_EXCEPTION"));
        final Set<Role> allRoles = new HashSet<>();
        Role role = this.roleRepository.getRoleByName(SelfServiceApiConstants.SELF_SERVICE_USER_ROLE);
        if (role == null) {
            throw new RoleNotFoundException(SelfServiceApiConstants.SELF_SERVICE_USER_ROLE);
        }
        allRoles.add(role);
        List<Client> clients = new ArrayList<>();
        User user = new User(reg.getUsername(), password, authorities);
        AppSelfServiceUser appUser = new AppSelfServiceUser(client.getOffice(), user, allRoles, reg.getEmail(), client.getFirstname(),
                client.getLastname(), null, passwordNeverExpire, isSelfServiceUser, clients, null);
        this.userDomainService.create(appUser, true);
        this.appUserClientMappingRepository.saveClientUserMapping(appUser.getId(), client.getId());
        return appUser;
    }

    private RuntimeException handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve, String username) {
        Throwable causeToEvaluate = realCause != null ? realCause : dve;
        if (causeToEvaluate.getMessage() != null && causeToEvaluate.getMessage().contains("'username_org'")) {
            return new PlatformDataIntegrityException("error.msg.user.duplicate.username", 
                    "User with username " + username + " already exists.", "username", username);
        }
        return ErrorHandler.getMappable(dve, "error.msg.unknown.data.integrity.issue", "Unknown data integrity issue with resource.");
    }
}