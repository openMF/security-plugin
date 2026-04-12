/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.selfservice.registration.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.response.Response;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.fineract.selfservice.testing.support.SelfServiceIntegrationTestBase;
import org.apache.fineract.selfservice.testing.support.SelfServiceTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SelfServiceEnrollmentIntegrationTest extends SelfServiceIntegrationTestBase {

    private static final String ENROLLMENT_PATH = "/fineract-provider/api/v1/self/registration/client-user";

    private String generateUniquePayload() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        return """
            {
              "username": "user_%s",
              "password": "Password123!",
              "firstName": "Test",
              "lastName": "User",
              "mobileNumber": "555%s",
              "email": "test%s@fineract.test",
              "authenticationMode": "email",
              "active": true
            }
            """.formatted(unique, unique, unique);
    }
    
    private String generateDuplicateMobilePayload(String unique) {
        return """
            {
              "username": "diffuser_%s",
              "password": "Password123!",
              "firstName": "Test",
              "lastName": "User",
              "mobileNumber": "555%s",
              "email": "diff%s@fineract.test",
              "authenticationMode": "email",
              "active": true
            }
            """.formatted(unique, unique, unique);
    }

    private Response executeSelfEnrollment(String payload, CountDownLatch latch) {
        try {
            if (latch != null) {
                latch.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return given(SelfServiceTestUtils.requestSpec(getFineractPort()))
            .body(payload)
            .when()
            .post(ENROLLMENT_PATH);
    }

    @Test
    @DisplayName("Successfully enrolls an atomic Client and User")
    void testSuccessfulSelfEnrollment() {
        String payload = generateUniquePayload();
        Response response = given(SelfServiceTestUtils.requestSpec(getFineractPort()))
            .body(payload)
            .when()
            .post(ENROLLMENT_PATH);

        assertEquals(200, response.getStatusCode(), "Expected successful enrollment");
    }

    @Test
    @DisplayName("Fails enrollment due to duplicate mobile number with 409 Conflict")
    void testDuplicateMobileNumberFailsConstraint() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String payload1 = """
            {
              "username": "user1_%s",
              "password": "Password123!",
              "firstName": "Test",
              "lastName": "User",
              "mobileNumber": "555%s",
              "email": "test1%s@fineract.test",
              "authenticationMode": "email",
              "active": true
            }
            """.formatted(unique, unique, unique);

        // First enroll should succeed
        Response response1 = executeSelfEnrollment(payload1, null);
        assertEquals(200, response1.getStatusCode());

        String payload2 = generateDuplicateMobilePayload(unique);
        
        // Second enroll should fail with 409 constraint violation
        Response response2 = executeSelfEnrollment(payload2, null);
        assertEquals(409, response2.getStatusCode(), "Expected 409 conflict for duplicate mobile number");
    }

    @Test
    @DisplayName("Concurrent enrollment avoids partial DB state via transactional isolation")
    void testConcurrentRaceCondition() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String payload1 = generateDuplicateMobilePayload(unique);
        String payload2 = generateDuplicateMobilePayload(unique);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        CompletableFuture<Response> req1 = CompletableFuture.supplyAsync(() -> executeSelfEnrollment(payload1, latch), executor);
        CompletableFuture<Response> req2 = CompletableFuture.supplyAsync(() -> executeSelfEnrollment(payload2, latch), executor);

        // Release the latch to start both concurrently
        latch.countDown();

        Response r1 = req1.join();
        Response r2 = req2.join();

        int status1 = r1.getStatusCode();
        int status2 = r2.getStatusCode();

        // At least one succeeds or fails, but BOTH cannot succeed
        assertTrue(
            (status1 == 200 && status2 == 409) || 
            (status1 == 409 && status2 == 200) || 
            (status1 == 409 && status2 == 409),
            "Expected race condition to resolve as atomic commit/fail. Got statuses: " + status1 + " and " + status2
        );
    }
}
