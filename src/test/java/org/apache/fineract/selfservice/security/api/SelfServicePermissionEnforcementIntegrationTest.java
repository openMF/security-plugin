/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.selfservice.security.api;

import static io.restassured.RestAssured.given;

import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.fineract.selfservice.testing.support.SelfServiceIntegrationTestBase;
import org.apache.fineract.selfservice.testing.support.SelfServiceTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SelfServicePermissionEnforcementIntegrationTest extends SelfServiceIntegrationTestBase {

  @Test
  @DisplayName("Verify that Self-Service Users strictly require explicit READ_SAVINGSPRODUCT grant to access /v1/self/savingsproducts")
  void testSavingsProductsRequireReadSavingsProductPermission() throws Exception {
    
    // 1. Create a Client
    String clientName = UUID.randomUUID().toString().substring(0, 8);
    Map<String, Object> clientBody = new HashMap<>();
    clientBody.put("officeId", 1);
    clientBody.put("legalFormId", 1);
    clientBody.put("firstname", "Test");
    clientBody.put("lastname", clientName);
    clientBody.put("externalId", clientName);
    clientBody.put("dateFormat", "dd MMMM yyyy");
    clientBody.put("locale", "en");
    clientBody.put("active", true);
    clientBody.put("activationDate", "01 January 2026");

    Integer clientId = given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "mifos", "password"))
        .body(clientBody)
        .post(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/clients")
        .then()
        .statusCode(200)
        .extract()
        .path("clientId");

    // Fetch the client accountNo
    String accountNo = given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "mifos", "password"))
        .get(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/clients/" + clientId)
        .then()
        .statusCode(200)
        .extract()
        .path("accountNo");

    // 2. Clear ALL_FUNCTIONS and ALL_FUNCTIONS_READ from 'Self Service User' role, and ensure READ_SAVINGSPRODUCT is false
    Response getRolesResponse = given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "mifos", "password"))
        .get(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/roles");
    Integer roleId = getRolesResponse.jsonPath().getInt("find { it.name == 'Self Service User' }.id");

    Map<String, Object> permissions = new HashMap<>();
    permissions.put("ALL_FUNCTIONS", false);
    permissions.put("ALL_FUNCTIONS_READ", false);
    permissions.put("READ_SAVINGSPRODUCT", false);

    Map<String, Object> permissionBody = new HashMap<>();
    permissionBody.put("permissions", permissions);

    given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "mifos", "password"))
        .body(permissionBody)
        .put(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/roles/" + roleId + "/permissions")
        .then()
        .statusCode(200);

    // 3. Initiate Self Service Registration
    String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
    
    Map<String, Object> regBody = new HashMap<>();
    regBody.put("accountNumber", accountNo);
    regBody.put("firstName", "Test");
    regBody.put("lastName", clientName);
    regBody.put("username", username);
    regBody.put("password", "Strong#Abc123");
    regBody.put("email", username + "@fineract.org");
    regBody.put("authenticationMode", "email");

    // This will create a registration request but might fail sending the email. We just need the DB record.
    // However, if the email send fails, the transaction is rolled back!
    // So actually, let's mock or use the AppUser route... wait, if the transaction rolls back, we can't extract the token!
    // BUT we CAN just INSERT the AppSelfServiceUser directly using JDBC!

    Properties props = new Properties();
    props.setProperty("user", "postgres");
    props.setProperty("password", "postgres");

    String jdbcUrl = postgres.getJdbcUrl();

    try (java.sql.Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, props)) {
        conn.setAutoCommit(false);
        try {
            long userId;
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT setval(pg_get_serial_sequence('m_appuser', 'id'), "
                            + "GREATEST(COALESCE((SELECT MAX(id) FROM m_appuser), 0), COALESCE((SELECT MAX(id) FROM m_appselfservice_user), 0)))")) {
                ps.execute();
            }

            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO m_appuser(office_id, username, password, email, firstname, lastname, is_deleted, nonexpired, nonlocked, nonexpired_credentials, enabled, firsttime_login_remaining) "
                            + "VALUES (1, ?, (SELECT password FROM m_appuser WHERE username='mifos' LIMIT 1), ?, 'Tomas', 'Test', false, true, true, true, true, false) RETURNING id")) {
                ps.setString(1, username);
                ps.setString(2, username + "@fineract.org");
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    userId = rs.getLong(1);
                }
            }

            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO m_appuser_role(appuser_id, role_id) VALUES (?, ?)")) {
                ps.setLong(1, userId);
                ps.setInt(2, roleId);
                ps.executeUpdate();
            }

            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO m_appselfservice_user(id, office_id, username, password, email, firstname, lastname, nonexpired, nonlocked, nonexpired_credentials, enabled, firsttime_login_remaining, password_never_expires, is_self_service_user, password_reset_required) "
                            + "SELECT id, office_id, username, password, email, firstname, lastname, nonexpired, nonlocked, nonexpired_credentials, enabled, firsttime_login_remaining, true, true, false FROM m_appuser WHERE id = ?")) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            }

            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT setval(pg_get_serial_sequence('m_appuser', 'id'), (SELECT MAX(id) FROM m_appuser))")) {
                ps.execute();
            }

            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT setval(pg_get_serial_sequence('m_appselfservice_user', 'id'), (SELECT MAX(id) FROM m_appselfservice_user))")) {
                ps.execute();
            }

            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO m_appselfservice_user_role(appuser_id, role_id) VALUES (?, ?)")) {
                ps.setLong(1, userId);
                ps.setInt(2, roleId);
                ps.executeUpdate();
            }

            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO m_selfservice_user_client_mapping(appuser_id, client_id) VALUES (?, ?)")) {
                ps.setLong(1, userId);
                ps.setInt(2, clientId);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // 4. Test the API without the permission: Expect 403
    given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), username, "password"))
        .when()
        .get(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/self/savingsproducts")
        .then()
        .statusCode(403);

    // 4b. Even if ALL_FUNCTIONS is set, self-service must still require explicit READ_SAVINGSPRODUCT
    permissions.clear();
    permissions.put("ALL_FUNCTIONS", true);
    permissions.put("ALL_FUNCTIONS_READ", true);
    permissions.put("READ_SAVINGSPRODUCT", false);
    permissionBody.put("permissions", permissions);

    given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "mifos", "password"))
        .body(permissionBody)
        .put(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/roles/" + roleId + "/permissions")
        .then()
        .statusCode(200);

    given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), username, "password"))
        .when()
        .get(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/self/savingsproducts")
        .then()
        .statusCode(403);

    // 5. Grant READ_SAVINGSPRODUCT
    permissions.clear();
    permissions.put("READ_SAVINGSPRODUCT", true);
    permissionBody.put("permissions", permissions);

    given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "mifos", "password"))
        .body(permissionBody)
        .put(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/roles/" + roleId + "/permissions")
        .then()
        .statusCode(200);

    // 6. Test the API with the permission: Expect 200
    given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), username, "password"))
        .when()
        .get(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/self/savingsproducts")
        .then()
        .statusCode(200);

    // 7. Revoke READ_SAVINGSPRODUCT to verify the cache does not retain it
    permissions.put("READ_SAVINGSPRODUCT", false);
    permissionBody.put("permissions", permissions);

    given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "mifos", "password"))
        .body(permissionBody)
        .put(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/roles/" + roleId + "/permissions")
        .then()
        .statusCode(200);

    // 8. Test the API without the permission: Expect 403 immediately
    given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), username, "password"))
        .when()
        .get(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/self/savingsproducts")
        .then()
        .statusCode(403);
  }
}
