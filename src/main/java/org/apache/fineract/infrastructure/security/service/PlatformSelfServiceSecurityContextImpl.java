/*
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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.apache.fineract.infrastructure.security.service;

import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.useradministration.domain.AppSelfServiceUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Implementation of PlatformSelfServiceSecurityContext for the Self Service Plugin.
 * This class provides the missing bean that Fineract core expects for self-service APIs.
 */
@Component
public class PlatformSelfServiceSecurityContextImpl implements PlatformSelfServiceSecurityContext {

    @Override
    public AppSelfServiceUser authenticatedSelfServiceUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new PlatformApiDataValidationException("error.msg.self.service.user.not.authenticated",
                    "Self service user is not authenticated", null);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AppSelfServiceUser appSelfServiceUser) {
            return appSelfServiceUser;
        }

        throw new PlatformApiDataValidationException("error.msg.self.service.user.not.found",
                "No self service user found in security context", null);
    }

    @Override
    public AppSelfServiceUser getAuthenticatedSelfServiceUserIfPresent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof AppSelfServiceUser appSelfServiceUser) {
                return appSelfServiceUser;
            }
        }
        return null;
    }

    @Override
    public AppSelfServiceUser authenticatedUser(CommandWrapper commandWrapper) {
        return authenticatedSelfServiceUser();
    }

    @Override
    public void validateAccessRights(String resourceOfficeHierarchy) {
        authenticatedSelfServiceUser();   // Ensure the user is properly authenticated
    }

    @Override
    public String officeHierarchy() {
        return null;   // Self-service users typically do not use office hierarchy
    }

    @Override
    public boolean doesPasswordHasToBeRenewed(AppSelfServiceUser currentSelfServiceUser) {
        return false;  // You can implement custom logic here if needed
    }

    /**
     * Required by PlatformUserRightsContext (the parent interface)
     */
    @Override
    public void isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
    }
}