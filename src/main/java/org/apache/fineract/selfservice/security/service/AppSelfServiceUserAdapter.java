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

import java.lang.reflect.Field;
import java.util.HashSet;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUser;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.security.core.userdetails.User;

/**
 * Creates a minimal, read-only {@link AppUser} stub from an {@link AppSelfServiceUser}.
 *
 * This adapter exists solely to pass the {@code context.authenticatedUser()} guard checks in core
 * Fineract read services. The stub is NOT a persistent entity and must never be used for write
 * operations.
 */
final class AppSelfServiceUserAdapter {

  private AppSelfServiceUserAdapter() {}

  static AppUser fromSelfServiceUser(AppSelfServiceUser selfServiceUser) {
    User springUser = new User(
        selfServiceUser.getUsername(),
        selfServiceUser.getPassword(),
        selfServiceUser.isEnabled(),
        selfServiceUser.isAccountNonExpired(),
        selfServiceUser.isCredentialsNonExpired(),
        selfServiceUser.isAccountNonLocked(),
        selfServiceUser.getAuthorities()
    );

    AppUser stub = new AppUser(
        selfServiceUser.getOffice(),
        springUser,
        new HashSet<>(),
        selfServiceUser.getEmail(),
        selfServiceUser.getFirstname(),
        selfServiceUser.getLastname(),
        null,
        true,
        false
    );
    setId(stub, selfServiceUser.getId());
    return stub;
  }

  private static void setId(AppUser stub, Long id) {
    try {
      Field idField = AppUser.class.getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(stub, id);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException("Failed to set id on AppUser stub", e);
    }
  }
}
