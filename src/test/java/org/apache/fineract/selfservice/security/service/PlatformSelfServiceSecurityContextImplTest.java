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
package org.apache.fineract.selfservice.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUser;
import org.apache.fineract.useradministration.domain.Permission;
import org.apache.fineract.useradministration.domain.Role;
import org.apache.fineract.useradministration.exception.UnAuthenticatedUserException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class PlatformSelfServiceSecurityContextImplTest {

  private PlatformSelfServiceSecurityContextImpl securityContext;
  private ConfigurationDomainService configService;

  @BeforeEach
  void setUp() {
    FineractPlatformTenant tenant =
        new FineractPlatformTenant(1L, "default", "Default Tenant", "UTC", null);
    ThreadLocalContextUtil.setTenant(tenant);
    HashMap<BusinessDateType, LocalDate> businessDates = new HashMap<>();
    businessDates.put(BusinessDateType.BUSINESS_DATE, LocalDate.now());
    businessDates.put(BusinessDateType.COB_DATE, LocalDate.now().minusDays(1));
    ThreadLocalContextUtil.setBusinessDates(businessDates);

    configService = mock(ConfigurationDomainService.class);
    when(configService.isPasswordForcedResetEnable()).thenReturn(false);
    securityContext = new PlatformSelfServiceSecurityContextImpl(configService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    ThreadLocalContextUtil.clearTenant();
  }

  private AppSelfServiceUser createAndSetSelfServiceUser(Set<Role> roles) {
    Office office = mock(Office.class);
    when(office.getId()).thenReturn(1L);
    when(office.getHierarchy()).thenReturn(".");

    Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
    for (Role role : roles) {
      for (Permission perm : role.getPermissions()) {
        authorities.add(new SimpleGrantedAuthority(perm.getCode()));
      }
    }
    if (authorities.isEmpty()) {
      authorities.add(new SimpleGrantedAuthority("DUMMY_ROLE"));
    }

    User springUser = new User("ssuser", "password123", true, true, true, true, authorities);
    AppSelfServiceUser ssUser = new AppSelfServiceUser(
        office, springUser, roles, "ss@test.com", "Self", "User", null, true, true,
        new ArrayList<Client>(), false);

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(ssUser, null, ssUser.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);

    return ssUser;
  }

  private void setAuthenticatedPrincipal(AppSelfServiceUser principal) {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  private Role createRoleWithPermissions(String roleName, String... permissionCodes) {
    Role role = mock(Role.class);
    when(role.getId()).thenReturn(1L);
    when(role.getName()).thenReturn(roleName);
    when(role.isDisabled()).thenReturn(false);

    Set<Permission> permissions = new HashSet<>();
    for (String code : permissionCodes) {
      Permission perm = mock(Permission.class);
      when(perm.getCode()).thenReturn(code);
      permissions.add(perm);
      when(role.hasPermissionTo(code)).thenReturn(true);
    }
    when(role.getPermissions()).thenReturn(permissions);
    return role;
  }

  // --- authenticatedSelfServiceUser ---

  @Test
  void authenticatedSelfServiceUser_shouldReturnUserWhenPrincipalIsSet() {
    AppSelfServiceUser ssUser = createAndSetSelfServiceUser(new HashSet<>());
    AppSelfServiceUser result = securityContext.authenticatedSelfServiceUser();
    assertThat(result).isSameAs(ssUser);
  }

  @Test
  void authenticatedSelfServiceUser_shouldThrowWhenNoPrincipal() {
    // No authentication set
    assertThatThrownBy(() -> securityContext.authenticatedSelfServiceUser())
        .isInstanceOf(UnAuthenticatedUserException.class);
  }

  @Test
  void authenticatedSelfServiceUser_shouldThrowWhenPrincipalIsNotSelfServiceUser() {
    // Set a non-AppSelfServiceUser principal
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken("anonymous", null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertThatThrownBy(() -> securityContext.authenticatedSelfServiceUser())
        .isInstanceOf(UnAuthenticatedUserException.class);
  }

  // --- validateHasReadPermission ---

  @Test
  void validateHasReadPermission_shouldDelegateToSelfServiceUser() {
    AppSelfServiceUser principal = mock(AppSelfServiceUser.class);
    when(principal.getAuthorities()).thenReturn(new ArrayList<>());
    setAuthenticatedPrincipal(principal);
    securityContext.validateHasReadPermission("CLIENT");
    verify(principal).validateHasReadPermission("CLIENT");
  }

  @Test
  void validateHasReadPermission_shouldThrowWhenNotAuthenticated() {
    assertThatThrownBy(() -> securityContext.validateHasReadPermission("CLIENT"))
        .isInstanceOf(UnAuthenticatedUserException.class);
  }

  // --- validateHasCreatePermission ---

  @Test
  void validateHasCreatePermission_shouldDelegateToSelfServiceUser() {
    AppSelfServiceUser principal = mock(AppSelfServiceUser.class);
    when(principal.getAuthorities()).thenReturn(new ArrayList<>());
    setAuthenticatedPrincipal(principal);
    securityContext.validateHasCreatePermission("CLIENTIMAGE");
    verify(principal).validateHasCreatePermission("CLIENTIMAGE");
  }

  // --- validateHasDeletePermission ---

  @Test
  void validateHasDeletePermission_shouldDelegateToSelfServiceUser() {
    AppSelfServiceUser principal = mock(AppSelfServiceUser.class);
    when(principal.getAuthorities()).thenReturn(new ArrayList<>());
    setAuthenticatedPrincipal(principal);
    securityContext.validateHasDeletePermission("CLIENTIMAGE");
    verify(principal).validateHasDeletePermission("CLIENTIMAGE");
  }

  // --- isAuthenticated ---

  @Test
  void isAuthenticated_shouldNotThrowWhenSelfServiceUserIsPresent() {
    createAndSetSelfServiceUser(new HashSet<>());
    // Should not throw
    securityContext.isAuthenticated();
  }

  @Test
  void isAuthenticated_shouldThrowWhenNoPrincipal() {
    assertThatThrownBy(() -> securityContext.isAuthenticated())
        .isInstanceOf(UnAuthenticatedUserException.class);
  }

  // --- getAuthenticatedUserIfPresent ---

  @Test
  void getAuthenticatedUserIfPresent_shouldReturnUserWhenPresent() {
    AppSelfServiceUser ssUser = createAndSetSelfServiceUser(new HashSet<>());
    AppSelfServiceUser result = securityContext.getAuthenticatedUserIfPresent();
    assertThat(result).isSameAs(ssUser);
  }

  @Test
  void getAuthenticatedUserIfPresent_shouldReturnNullWhenNoPrincipal() {
    AppSelfServiceUser result = securityContext.getAuthenticatedUserIfPresent();
    assertThat(result).isNull();
  }

  // --- validateAccessRights ---

  @Test
  void validateAccessRights_shouldPassWhenHierarchyMatches() {
    createAndSetSelfServiceUser(new HashSet<>());
    // Office hierarchy is "." so "." starts with "."
    securityContext.validateAccessRights(".");
  }

  // --- officeHierarchy ---

  @Test
  void officeHierarchy_shouldReturnUserOfficeHierarchy() {
    createAndSetSelfServiceUser(new HashSet<>());
    assertThat(securityContext.officeHierarchy()).isEqualTo(".");
  }
}
