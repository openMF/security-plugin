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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUser;
import org.apache.fineract.useradministration.domain.AppUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SelfServiceCompatibleSecurityContextTest {

  @BeforeEach
  void setUpTenantContext() {
    FineractPlatformTenant tenant =
        new FineractPlatformTenant(1L, "default", "Default Tenant", "UTC", null);
    ThreadLocalContextUtil.setTenant(tenant);
    HashMap<BusinessDateType, LocalDate> businessDates = new HashMap<>();
    businessDates.put(BusinessDateType.BUSINESS_DATE, LocalDate.now());
    businessDates.put(BusinessDateType.COB_DATE, LocalDate.now().minusDays(1));
    ThreadLocalContextUtil.setBusinessDates(businessDates);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    ThreadLocalContextUtil.clearTenant();
  }

  @Test
  void authenticatedUser_shouldReturnAppUserStubForSelfServicePrincipal() {
    ConfigurationDomainService config = mock(ConfigurationDomainService.class);
    SelfServiceCompatibleSecurityContext ctx = new SelfServiceCompatibleSecurityContext(config);

    Office office = mock(Office.class);
    when(office.getId()).thenReturn(1L);

    AppSelfServiceUser principal = mock(AppSelfServiceUser.class);
    when(principal.getId()).thenReturn(42L);
    when(principal.getOffice()).thenReturn(office);
    when(principal.getUsername()).thenReturn("ssuser");
    when(principal.getPassword()).thenReturn("secret");
    when(principal.isEnabled()).thenReturn(true);
    when(principal.isAccountNonExpired()).thenReturn(true);
    when(principal.isCredentialsNonExpired()).thenReturn(true);
    when(principal.isAccountNonLocked()).thenReturn(true);
    when(principal.getAuthorities()).thenReturn(new ArrayList<>());
    when(principal.getRoles()).thenReturn(new HashSet<>());
    when(principal.getEmail()).thenReturn("ss@test.com");
    when(principal.getFirstname()).thenReturn("Self");
    when(principal.getLastname()).thenReturn("User");

    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));

    AppUser stub = ctx.authenticatedUser();
    assertThat(stub.getId()).isEqualTo(42L);
    assertThat(stub.getUsername()).isEqualTo("ssuser");
    assertThat(stub.getOffice()).isSameAs(office);
  }

  @Test
  void getAuthenticatedUserIfPresent_shouldReturnAppUserStubForSelfServicePrincipal() {
    ConfigurationDomainService config = mock(ConfigurationDomainService.class);
    SelfServiceCompatibleSecurityContext ctx = new SelfServiceCompatibleSecurityContext(config);

    Office office = mock(Office.class);
    when(office.getId()).thenReturn(1L);

    AppSelfServiceUser principal = mock(AppSelfServiceUser.class);
    when(principal.getId()).thenReturn(7L);
    when(principal.getOffice()).thenReturn(office);
    when(principal.getUsername()).thenReturn("ssuser2");
    when(principal.getPassword()).thenReturn("secret");
    when(principal.isEnabled()).thenReturn(true);
    when(principal.isAccountNonExpired()).thenReturn(true);
    when(principal.isCredentialsNonExpired()).thenReturn(true);
    when(principal.isAccountNonLocked()).thenReturn(true);
    when(principal.getAuthorities()).thenReturn(new ArrayList<>());
    when(principal.getRoles()).thenReturn(new HashSet<>());
    when(principal.getEmail()).thenReturn("ss2@test.com");
    when(principal.getFirstname()).thenReturn("Self");
    when(principal.getLastname()).thenReturn("User2");

    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));

    AppUser stub = ctx.getAuthenticatedUserIfPresent();
    assertThat(stub.getId()).isEqualTo(7L);
    assertThat(stub.getUsername()).isEqualTo("ssuser2");
    assertThat(stub.getOffice()).isSameAs(office);
  }
}

