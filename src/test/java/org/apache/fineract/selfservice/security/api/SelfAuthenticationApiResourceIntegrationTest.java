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
package org.apache.fineract.selfservice.security.api;

import static io.restassured.RestAssured.given;

import org.apache.fineract.selfservice.testing.support.SelfServiceIntegrationTestBase;
import org.apache.fineract.selfservice.testing.support.SelfServiceTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration Test ensuring the Spring Context starts, Testcontainers database connects,
 * and RestAssured correctly maps HTTP traffic to the embedded Fineract endpoints.
 *
 * <p>Named *IntegrationTest.java so it is executed during the Failsafe 'verify' phase.
 */
class SelfAuthenticationApiResourceIntegrationTest extends SelfServiceIntegrationTestBase {

  @Test
  @DisplayName("POST /v1/self/authentication with missing credentials returns 500")
  void authenticate_missingCredentials_returns500() {
    // Proves the web server is up and the endpoint rejects empty credentials at the filter layer
    String emptyBody = "{}";

    given(SelfServiceTestUtils.requestSpec(getFineractPort()))
        .body(emptyBody)
    .when()
        .post(SelfServiceTestUtils.SELF_AUTH_PATH)
    .then()
        .statusCode(500);
  }

  @Test
  @DisplayName("POST /v1/self/authentication with invalid credentials returns 401 Unauthorized")
  void authenticate_invalidCredentials_returns401() {
    // Proves that the Spring Security filter chain and the custom Self Service 
    // Authentication provider are actively rejecting bad credentials
    String invalidBody = "{\"username\":\"fakeUser\",\"password\":\"fakePass\"}";

    given(SelfServiceTestUtils.requestSpec(getFineractPort()))
        .body(invalidBody)
    .when()
        .post(SelfServiceTestUtils.SELF_AUTH_PATH)
    .then()
        .statusCode(401);
  }
}
