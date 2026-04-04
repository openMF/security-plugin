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
package org.apache.fineract.selfservice.security.filter;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.AuthenticationEntryPoint;

class SelfServiceBasicAuthenticationFilterTest {

    @Test
    void onSuccessfulAuthentication_doesNotThrow_withAppSelfServiceUserPrincipal() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication auth = mock(Authentication.class);
        AppSelfServiceUser selfServiceUser = mock(AppSelfServiceUser.class);
        when(auth.getPrincipal()).thenReturn(selfServiceUser);

        SelfServiceBasicAuthenticationFilter filter = new SelfServiceBasicAuthenticationFilter(
                mock(AuthenticationManager.class), mock(AuthenticationEntryPoint.class),
                null, null, null, null, null, null);

        assertThatCode(() -> filter.onSuccessfulAuthentication(request, response, auth))
                .doesNotThrowAnyException();
    }
}
