package org.apache.fineract.selfservice.runreport;

import static io.restassured.RestAssured.given;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.fineract.selfservice.testing.support.SelfServiceIntegrationTestBase;
import org.apache.fineract.selfservice.testing.support.SelfServiceTestUtils;
import org.apache.fineract.selfservice.registration.SelfServiceApiConstants;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Container-level integration tests for the self-service runreport wrapper.
 *
 * <p>These tests assume that the underlying Fineract instance has at least one
 * report named "Client Details" configured and that the
 * fineract.modules.selfservice.runreports.allowlist property is set to include
 * that name for the test profile.</p>
 */
public class SelfRunReportIntegrationTest extends SelfServiceIntegrationTestBase {

  @Test
  @DisplayName("Self-service runreports deny non-allowlisted report names")
  void runReport_nonAllowlistedReport_denied() {
    // 1. Create a client using the superuser
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

    Integer clientId =
        given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "mifos", "password"))
            .body(clientBody)
            .post(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/clients")
            .then()
            .statusCode(200)
            .extract()
            .path("clientId");

    // 2. Find "Self Service User" role
    Response rolesResponse =
        given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "mifos", "password"))
            .get(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/roles");
    Integer roleId = rolesResponse.jsonPath().getInt("find { it.name == '" + SelfServiceApiConstants.SELF_SERVICE_USER_ROLE + "' }.id");
    if (roleId == null || roleId <= 0) {
      throw new IllegalStateException(
          "Could not resolve role id for '" + SelfServiceApiConstants.SELF_SERVICE_USER_ROLE + "'");
    }

    // 3. Create a self-service user mapped to the client + self-service role using JDBC
    //    This mirrors SelfServicePermissionEnforcementIntegrationTest.
    Properties props = new Properties();
    props.setProperty("user", "postgres");
    props.setProperty("password", "postgres");

    String jdbcUrl = postgres.getJdbcUrl();
    String username = "user_" + UUID.randomUUID().toString().substring(0, 8);

    try (Connection conn = DriverManager.getConnection(jdbcUrl, props)) {
      try (Statement st = conn.createStatement()) {
        String insertUser =
            "INSERT INTO m_appuser(office_id, username, password, email, firstname, lastname, "
                + "is_deleted, nonexpired, nonlocked, nonexpired_credentials, enabled, firsttime_login_remaining) "
                + "VALUES (1, '"
                + username
                + "', (SELECT password FROM m_appuser WHERE username='mifos' LIMIT 1), '"
                + username
                + "@fineract.org', 'User', 'Test', false, true, true, true, true, false) RETURNING id";

        ResultSet rs = st.executeQuery(insertUser);
        rs.next();
        long appUserId = rs.getLong(1);

        st.execute("INSERT INTO m_appuser_role(appuser_id, role_id) VALUES (" + appUserId + ", " + roleId + ")");

        // Insert into plugin m_appselfservice_user
        st.execute(
            "INSERT INTO m_appselfservice_user(id, office_id, username, password, email, firstname, lastname, "
                + "nonexpired, nonlocked, nonexpired_credentials, enabled, firsttime_login_remaining, is_self_service_user, is_deleted) "
                + "SELECT id, office_id, username, password, email, firstname, lastname, "
                + "nonexpired, nonlocked, nonexpired_credentials, enabled, firsttime_login_remaining, true, false "
                + "FROM m_appuser WHERE id = " + appUserId);

        // Map SelfService User to Role
        st.execute(
            "INSERT INTO m_appselfservice_user_role(appuser_id, role_id) VALUES (" + appUserId + ", " + roleId + ")");

        // Map SelfService User to Client
        st.execute(
            "INSERT INTO m_selfservice_user_client_mapping(appuser_id, client_id) VALUES (" + appUserId + ", " + clientId + ")");
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to seed self-service user for runreport IT", e);
    }

    // Ensure the seeded user can authenticate via the self-service authentication endpoint.
    // If this fails, the subsequent /self/runreports call will also return 401.
    given(SelfServiceTestUtils.requestSpec(getFineractPort()))
        .body("{\"username\":\"" + username + "\",\"password\":\"password\"}")
    .when()
        .post(SelfServiceTestUtils.SELF_AUTH_PATH)
        .then()
        .statusCode(200);

    // 4. Self-service user with a non-allowlisted report must be denied
    // Sanity check: ensure Basic Auth works for another self-service endpoint.
    // If this returned 401, the seeded user isn't being authenticated by the filter chain.
    given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), username, "password"))
        .when()
        .get(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/self/savingsproducts")
        .then()
        .statusCode(200);

    Response runReportResponse =
        given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), username, "password"))
            .when()
            .get(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/self/runreports/SomeUnknownReport")
            .then()
            .extract()
            .response();

    Assertions.assertEquals(
        403,
        runReportResponse.statusCode(),
        "Unexpected response body: " + runReportResponse.body().asString());
    Assertions.assertTrue(
        runReportResponse
            .body()
            .asString()
            .contains("Self-service is not permitted to run this report: SomeUnknownReport"));
  }
}

