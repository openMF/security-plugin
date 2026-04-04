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
package org.apache.fineract.selfservice.security.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.selfservice.security.exception.SelfServiceNoAuthorizationException;
import org.apache.fineract.selfservice.security.exception.SelfServiceResetPasswordException;
import org.apache.fineract.selfservice.security.exception.SelfServiceUnAuthenticatedUserException;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUser;
import org.apache.fineract.useradministration.exception.UnAuthenticatedUserException;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Primary 
@RequiredArgsConstructor
@Slf4j
public class PlatformSelfServiceSecurityContextImpl implements PlatformSelfServiceSecurityContext {

    private final ConfigurationDomainService configurationDomainService;

    protected static final List<CommandWrapper> EXEMPT_FROM_PASSWORD_RESET_CHECK = new ArrayList<CommandWrapper>(
            List.of(new CommandWrapperBuilder().changeUserPassword(null).build()));

    @Override
    public AppSelfServiceUser authenticatedSelfServiceUser() {
        
        log.warn("*************************************************************");
        log.warn("*                                                           *");
        log.warn("* public AppSelfServiceUser authenticatedSelfServiceUser()  *");
        log.warn("*                                                           *");
        log.warn("*************************************************************");

        AppSelfServiceUser currentUser = null;
        final SecurityContext context = SecurityContextHolder.getContext();
        if (context != null) {
            final Authentication auth = context.getAuthentication();
            if (auth != null) {
                Object principal = auth.getPrincipal();
                if (principal instanceof AppSelfServiceUser appUser) {
                    currentUser = appUser;
                }
            }
        }
        if (currentUser == null) {
            throw new UnAuthenticatedUserException();
        }
        if (this.doesPasswordHasToBeRenewed(currentUser)) {
            throw new SelfServiceResetPasswordException(currentUser.getId());
        }
        return currentUser;
    }

    @Override
    public void isAuthenticated() {
        authenticatedSelfServiceUser();
    }

    @Override
    public AppSelfServiceUser getAuthenticatedUserIfPresent() {
        
        log.warn("*************************************************************");
        log.warn("*                                                           *");
        log.warn("* public AppSelfServiceUser getAuthenticatedUserIfPresent() *");
        log.warn("*                                                           *");
        log.warn("*************************************************************");

        AppSelfServiceUser currentUser = null;
        final SecurityContext context = SecurityContextHolder.getContext();
        if (context != null) {
            final Authentication auth = context.getAuthentication();
            if (auth != null) {
                currentUser = (AppSelfServiceUser) auth.getPrincipal();
            }
        }
        if (currentUser == null) {
            return null;
        }
        if (this.doesPasswordHasToBeRenewed(currentUser)) {
            throw new SelfServiceResetPasswordException(currentUser.getId());
        }
        return currentUser;
    }

    @Override
    public AppSelfServiceUser authenticatedUser(CommandWrapper commandWrapper) {
        
        log.warn("******************************************************************************");
        log.warn("*                                                                            *");
        log.warn("* public AppSelfServiceUser authenticatedUser(CommandWrapper commandWrapper) *");
        log.warn("*                                                                            *");
        log.warn("******************************************************************************");

        AppSelfServiceUser currentUser = null;
        final SecurityContext context = SecurityContextHolder.getContext();
        if (context != null) {
            final Authentication auth = context.getAuthentication();
            if (auth != null) {
                currentUser = (AppSelfServiceUser) auth.getPrincipal();
            }
        }
        if (currentUser == null) {
            throw new SelfServiceUnAuthenticatedUserException();
        }
        if (this.shouldCheckForPasswordForceReset(commandWrapper, currentUser) && this.doesPasswordHasToBeRenewed(currentUser)) {
            throw new SelfServiceResetPasswordException(currentUser.getId());
        }
        return currentUser;
    }

    @Override
    public void validateAccessRights(final String resourceOfficeHierarchy) {

        final AppSelfServiceUser user = authenticatedSelfServiceUser();
        final String userOfficeHierarchy = user.getOffice().getHierarchy();

        if (!resourceOfficeHierarchy.startsWith(userOfficeHierarchy)) {
            throw new SelfServiceNoAuthorizationException("The user doesn't have enough permissions to access the resource.");
        }
    }

    @Override
    public String officeHierarchy() {
        return authenticatedSelfServiceUser().getOffice().getHierarchy();
    }

    @Override
    public boolean doesPasswordHasToBeRenewed(AppSelfServiceUser currentUser) {

        if (currentUser.isPasswordResetRequired()) {
            return true;
        }

        if (this.configurationDomainService.isPasswordForcedResetEnable() && !currentUser.getPasswordNeverExpires()) {

            Long passwordDurationDays = this.configurationDomainService.retrievePasswordLiveTime();
            final LocalDate passWordLastUpdateDate = currentUser.getLastTimePasswordUpdated();

            final LocalDate passwordExpirationDate = passWordLastUpdateDate.plusDays(passwordDurationDays);

            if (DateUtils.isBeforeTenantDate(passwordExpirationDate)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldCheckForPasswordForceReset(CommandWrapper commandWrapper, AppSelfServiceUser currentUser) {
        for (CommandWrapper commandItem : EXEMPT_FROM_PASSWORD_RESET_CHECK) {
            if (commandItem.actionName().equals(commandWrapper.actionName())
                    && commandItem.getEntityName().equals(commandWrapper.getEntityName())) {
                return commandWrapper.getEntityId() == null || !commandWrapper.getEntityId().equals(currentUser.getId());
            }
        }
        return true;
    }
}
