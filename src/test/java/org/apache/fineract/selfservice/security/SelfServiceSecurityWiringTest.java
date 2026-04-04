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
package org.apache.fineract.selfservice.security;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.apache.fineract.infrastructure.security.service.TenantAwareJpaPlatformUserDetailsService;
import org.apache.fineract.selfservice.security.service.TenantAwareJpaPlatformSelfServiceUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SelfServiceSecurityTestConfig.class)
@TestPropertySource("classpath:application-test.properties")
@WebAppConfiguration
class SelfServiceSecurityWiringTest {

    @Autowired
    private ApplicationContext ctx;

    @Qualifier("selfServiceAuthenticationProvider")
    @Autowired
    private DaoAuthenticationProvider selfServiceProvider;

    @Test
    void selfServiceAuthProvider_usesSelfServiceUserDetailsService() {
        UserDetailsService uds = (UserDetailsService)
                ReflectionTestUtils.getField(selfServiceProvider, "userDetailsService");
        assertInstanceOf(TenantAwareJpaPlatformSelfServiceUserDetailsService.class, uds);
    }

    @Test
    void selfServiceAuthProvider_isDistinctFromCoreProvider() {
        DaoAuthenticationProvider coreProvider =
                ctx.getBean("customAuthenticationProvider", DaoAuthenticationProvider.class);
        assertNotSame(coreProvider, selfServiceProvider);
    }

    @Test
    void coreAuthProvider_usesCoreUserDetailsService() {
        DaoAuthenticationProvider coreProvider =
                ctx.getBean("customAuthenticationProvider", DaoAuthenticationProvider.class);
        UserDetailsService uds = (UserDetailsService)
                ReflectionTestUtils.getField(coreProvider, "userDetailsService");
        assertInstanceOf(TenantAwareJpaPlatformUserDetailsService.class, uds);
    }

    @Test
    void selfServiceEntryPoint_exists() {
        assertNotNull(ctx.getBean("selfServiceBasicAuthenticationEntryPoint"));
    }

    @Test
    void selfServiceFilterChain_exists() {
        assertNotNull(ctx.getBean("selfServiceSecurityFilterChain"));
    }
}
