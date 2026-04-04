package org.apache.fineract.selfservice.security.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class PlatformSelfServiceSecurityContextImplTest {

  private final PlatformSelfServiceSecurityContextImpl context =
      new PlatformSelfServiceSecurityContextImpl();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authenticatedSelfServiceUser_shouldThrowWhenNoAuthentication() {
    SecurityContextHolder.clearContext();

    assertThrows(
        PlatformApiDataValidationException.class, () -> context.authenticatedSelfServiceUser());
  }

  @Test
  void authenticatedSelfServiceUser_shouldThrowWhenPrincipalIsNotSelfServiceUser() {
    // 3-arg constructor marks as authenticated, but principal is wrong type
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken("someUser", "password", Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertThrows(
        PlatformApiDataValidationException.class, () -> context.authenticatedSelfServiceUser());
  }

  @Test
  void authenticatedSelfServiceUser_shouldReturnUserWhenValid() {
    AppSelfServiceUser mockUser = mock(AppSelfServiceUser.class);
    // Must use 3-arg constructor to mark as authenticated
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(mockUser, "password", Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);

    AppSelfServiceUser result = context.authenticatedSelfServiceUser();
    assertSame(mockUser, result);
  }

  @Test
  void getAuthenticatedSelfServiceUserIfPresent_shouldReturnNullWhenNoAuth() {
    SecurityContextHolder.clearContext();

    assertNull(context.getAuthenticatedSelfServiceUserIfPresent());
  }

  @Test
  void getAuthenticatedSelfServiceUserIfPresent_shouldReturnNullWhenWrongPrincipal() {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "notSelfService", "password", Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertNull(context.getAuthenticatedSelfServiceUserIfPresent());
  }

  @Test
  void getAuthenticatedSelfServiceUserIfPresent_shouldReturnUserWhenValid() {
    AppSelfServiceUser mockUser = mock(AppSelfServiceUser.class);
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(mockUser, "password", Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);

    AppSelfServiceUser result = context.getAuthenticatedSelfServiceUserIfPresent();
    assertSame(mockUser, result);
  }

  @Test
  void authenticatedUser_shouldDelegateToAuthenticatedSelfServiceUser() {
    AppSelfServiceUser mockUser = mock(AppSelfServiceUser.class);
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(mockUser, "password", Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertSame(mockUser, context.authenticatedUser(null));
  }

  @Test
  void doesPasswordHasToBeRenewed_shouldAlwaysReturnFalse() {
    AppSelfServiceUser mockUser = mock(AppSelfServiceUser.class);
    assertFalse(context.doesPasswordHasToBeRenewed(mockUser));
  }

  @Test
  void officeHierarchy_shouldReturnNull() {
    assertNull(context.officeHierarchy());
  }

  @Test
  void validateAccessRights_shouldNotThrowWhenAuthenticated() {
    AppSelfServiceUser mockUser = mock(AppSelfServiceUser.class);
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(mockUser, "password", Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertDoesNotThrow(() -> context.validateAccessRights("."));
  }

  @Test
  void validateAccessRights_shouldThrowWhenNotAuthenticated() {
    SecurityContextHolder.clearContext();

    assertThrows(PlatformApiDataValidationException.class, () -> context.validateAccessRights("."));
  }
}
