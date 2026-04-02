package org.apache.fineract.infrastructure.security.service;

import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.useradministration.domain.AppSelfServiceUser;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Primary   // ← This is the key addition
public class PlatformSelfServiceSecurityContextImpl implements PlatformSelfServiceSecurityContext {
    
    
    @Override
    public AppSelfServiceUser authenticatedSelfServiceUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new PlatformApiDataValidationException("error.msg.self.service.user.not.authenticated",
                    "Self service user is not authenticated", null);
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof AppSelfServiceUser user) {
            return user;
        }

        throw new PlatformApiDataValidationException("error.msg.self.service.user.not.found",
                "No self service user found in security context", null);
    }

    @Override
    public AppSelfServiceUser getAuthenticatedSelfServiceUserIfPresent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof AppSelfServiceUser user) {
                return user;
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
        authenticatedSelfServiceUser();
    }

    @Override
    public String officeHierarchy() {
        return null;
    }

    @Override
    public boolean doesPasswordHasToBeRenewed(AppSelfServiceUser currentSelfServiceUser) {
        return false;
    }

    @Override
    public void isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();        
    }
}