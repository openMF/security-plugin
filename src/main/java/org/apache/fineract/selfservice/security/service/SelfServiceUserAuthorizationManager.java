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

import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUser;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

@Slf4j
public class SelfServiceUserAuthorizationManager
    implements AuthorizationManager<RequestAuthorizationContext> {

  @Override
  public AuthorizationDecision check(
      Supplier<Authentication> authentication, RequestAuthorizationContext fi) {
    if (!"OPTIONS".equalsIgnoreCase(fi.getRequest().getMethod())) {
      AppSelfServiceUser user = (AppSelfServiceUser) authentication.get().getPrincipal();
      log.warn("SELF SERVICE REQUEST");
      String pathURL = fi.getRequest().getRequestURL().toString();
      boolean isSelfServiceRequest = pathURL.contains("/self/");

      boolean notAllowed =
          ((isSelfServiceRequest && !user.isSelfServiceUser())
              || (!isSelfServiceRequest && user.isSelfServiceUser()));

      if (notAllowed) {
        log.warn("SELF SERVICE REQUEST NOT ALLOWED");
        return new AuthorizationDecision(false);
      }
      log.warn("SELF SERVICE REQUEST ALLOWED");
    }
    return new AuthorizationDecision(true);
  }

  public static SelfServiceUserAuthorizationManager selfServiceUserAuthManager() {
    return new SelfServiceUserAuthorizationManager();
  }
}
