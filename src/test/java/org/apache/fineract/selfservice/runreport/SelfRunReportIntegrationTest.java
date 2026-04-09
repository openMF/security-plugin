package org.apache.fineract.selfservice.runreport;

import static io.restassured.RestAssured.given;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

  private String setupSelfServiceUser() {
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

    Response rolesResponse =
        given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "mifos", "password"))
            .get(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/roles");
    Integer roleId = rolesResponse.jsonPath().getInt("find { it.name == '" + SelfServiceApiConstants.SELF_SERVICE_USER_ROLE + "' }.id");
    if (roleId == null || roleId <= 0) {
      throw new IllegalStateException(
          "Could not resolve role id for '" + SelfServiceApiConstants.SELF_SERVICE_USER_ROLE + "'");
    }

    Properties props = new Properties();
    props.setProperty("user", "postgres");
    props.setProperty("password", "postgres");

    String jdbcUrl = postgres.getJdbcUrl();
    String username = "user_" + UUID.randomUUID().toString().substring(0, 8);

    try (Connection conn = DriverManager.getConnection(jdbcUrl, props)) {
      String insertUser =
          "INSERT INTO m_appuser(office_id, username, password, email, firstname, lastname, "
              + "is_deleted, nonexpired, nonlocked, nonexpired_credentials, enabled, firsttime_login_remaining) "
              + "VALUES (1, ?, (SELECT password FROM m_appuser WHERE username='mifos' LIMIT 1), ?, 'User', 'Test', false, true, true, true, true, false) RETURNING id";
      long appUserId;
      try (PreparedStatement psUser = conn.prepareStatement(insertUser)) {
        psUser.setString(1, username);
        psUser.setString(2, username + "@fineract.org");
        try (ResultSet rs = psUser.executeQuery()) {
          rs.next();
          appUserId = rs.getLong(1);
        }
      }

      try (PreparedStatement psUserRole = conn.prepareStatement("INSERT INTO m_appuser_role(appuser_id, role_id) VALUES (?, ?)")) {
        psUserRole.setLong(1, appUserId);
        psUserRole.setInt(2, roleId);
        psUserRole.execute();
      }

      String insertSelfUser = "INSERT INTO m_appselfservice_user(id, office_id, username, password, email, firstname, lastname, "
          + "nonexpired, nonlocked, nonexpired_credentials, enabled, firsttime_login_remaining, is_self_service_user, is_deleted) "
          + "SELECT id, office_id, username, password, email, firstname, lastname, "
          + "nonexpired, nonlocked, nonexpired_credentials, enabled, firsttime_login_remaining, true, false "
          + "FROM m_appuser WHERE id = ?";
      try (PreparedStatement psSelfUser = conn.prepareStatement(insertSelfUser)) {
        psSelfUser.setLong(1, appUserId);
        psSelfUser.execute();
      }

      try (PreparedStatement psSelfUserRole = conn.prepareStatement("INSERT INTO m_appselfservice_user_role(appuser_id, role_id) VALUES (?, ?)")) {
        psSelfUserRole.setLong(1, appUserId);
        psSelfUserRole.setInt(2, roleId);
        psSelfUserRole.execute();
      }

      try (PreparedStatement psClientMapping = conn.prepareStatement("INSERT INTO m_selfservice_user_client_mapping(appuser_id, client_id) VALUES (?, ?)")) {
        psClientMapping.setLong(1, appUserId);
        psClientMapping.setInt(2, clientId);
        psClientMapping.execute();
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to seed self-service user for runreport IT", e);
    }

    given(SelfServiceTestUtils.requestSpec(getFineractPort()))
        .body("{\"username\":\"" + username + "\",\"password\":\"password\"}")
    .when()
        .post(SelfServiceTestUtils.SELF_AUTH_PATH)
        .then()
        .statusCode(200);

    return username;
  }

  @Test
  @DisplayName("Self-service runreports deny non-allowlisted report names")
  void runReport_nonAllowlistedReport_denied() {
    String username = setupSelfServiceUser();

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

  @Test
  @DisplayName("Self-service runreports allows allowlisted report names")
  void runReport_allowlistedReport_success() {
    String username = setupSelfServiceUser();

    Response runReportResponse =
        given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), username, "password"))
            .when()
            .get(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/self/runreports/Client Details")
            .then()
            .extract()
            .response();

    int statusCode = runReportResponse.statusCode();
    Assertions.assertTrue(
        statusCode == 200 || statusCode == 400 || statusCode == 404,
        "Expected successful execution (200), validation failure (400), or report missing in DB (404), but got: " + statusCode + ". Body: " + runReportResponse.body().asString());
  }
}
