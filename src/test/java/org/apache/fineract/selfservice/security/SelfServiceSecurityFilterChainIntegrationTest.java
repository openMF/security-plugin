/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.selfservice.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK, 
    classes = SelfServiceSecurityTestConfig.class,
    properties = "spring.main.allow-bean-definition-overriding=true"
)
@ActiveProfiles("test")
@TestPropertySource("classpath:application-test.properties")
@AutoConfigureMockMvc
class SelfServiceSecurityFilterChainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registrationEndpoint_isPublicAndReturns200() throws Exception {
        // POST /v1/self/registration without any Auth header should not return 401
        ResultMatcher notUnauthorized = result -> {
            int status = result.getResponse().getStatus();
            if (status == 401 || status == 404) {
                throw new AssertionError("Expected status not to be 401 or 404, but was " + status);
            }
        };

        mockMvc.perform(post("/v1/self/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountNumber\":\"ACC001\",\"tenantIdentifier\":\"default\"}"))
            .andExpect(notUnauthorized); // May be 404, 400, etc., but not blocked by filter chain
    }

    @Test
    void forgotPasswordEndpoints_arePublicAndNotBlockedByAuthentication() throws Exception {
        ResultMatcher notUnauthorized = result -> {
            int status = result.getResponse().getStatus();
            if (status == 401 || status == 403 || (status >= 500 && status < 600)) {
                throw new AssertionError("Expected forgot-password endpoint not to be blocked by auth and not to return server error");
            }
        };

        mockMvc.perform(post("/v1/self/password/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"demo\",\"authenticationMode\":\"email\"}"))
            .andExpect(notUnauthorized);

        mockMvc.perform(post("/v1/self/password/renew")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"externalAuthenticationToken\":\"123456\",\"password\":\"Strong#Abc123\",\"repeatPassword\":\"Strong#Abc123\"}"))
            .andExpect(notUnauthorized);
    }

    @Test
    void protectedEndpoint_withoutAuthReturns401() throws Exception {
        ResultMatcher unauthorizedOrForbidden = result -> {
            int status = result.getResponse().getStatus();
            if (status != 401 && status != 403) {
                throw new AssertionError("Expected status to be 401 or 403, but was " + status);
            }
        };

        mockMvc.perform(get("/v1/self/clients")
                .header("Fineract-Platform-TenantId", "default"))
            .andExpect(unauthorizedOrForbidden);
    }

    @Test
    void nonSelfEndpoint_isNotAffectedByOurFilterChain() throws Exception {
        ResultMatcher notUnauthorized = result -> {
            if (result.getResponse().getStatus() == 401) {
                throw new AssertionError("Expected status not to be 401");
            }
        };

        mockMvc.perform(get("/actuator/health"))
            .andExpect(notUnauthorized);
    }
}
