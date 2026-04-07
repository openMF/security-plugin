package org.apache.fineract.selfservice.client.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SelfClientsApiIntegrationTest extends SelfServiceIntegrationTestBase {

  @Test
  @DisplayName("GET /v1/self/clients without auth returns 403")
  void retrieveAll_withoutAuth_returns403() {
    given(SelfServiceTestUtils.requestSpec(getFineractPort()))
        .when()
        .get(SelfServiceTestUtils.SELF_CLIENTS_PATH)
        .then()
        .statusCode(403);
  }

  @Test
  @DisplayName("GET /v1/self/clients with mifos returns 401 (Not a Self Service User)")
  void retrieveAll_withSuperUser_returns401IfNotSelfService() {
    given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "mifos", "password"))
        .when()
        .get(SelfServiceTestUtils.SELF_CLIENTS_PATH)
        .then()
        .statusCode(401);
  }

  @Test
  @DisplayName("Verify ?fields=savingsAccounts preserves internal fields and strips unwanted wrappers")
  void verifyFieldsParameterSerializationWorks() throws Exception {
    
    // 1. Create Client
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

    // 2. Create Savings Product
    Map<String, Object> productBody = new HashMap<>();
    productBody.put("name", "Savings " + clientName);
    productBody.put("shortName", clientName.substring(0, 4));
    productBody.put("description", "Desc");
    productBody.put("currencyCode", "USD");
    productBody.put("digitsAfterDecimal", 2);
    productBody.put("inMultiplesOf", 1);
    productBody.put("locale", "en");
    productBody.put("interestCompoundingPeriodType", 1);
    productBody.put("interestPostingPeriodType", 4);
    productBody.put("interestCalculationType", 1);
    productBody.put("interestCalculationDaysInYearType", 365);
    productBody.put("accountingRule", 1);
    productBody.put("nominalAnnualInterestRate", 5.0);

    Integer productId = given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "mifos", "password"))
        .body(productBody)
        .post(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/savingsproducts")
        .then()
        .statusCode(200)
        .extract()
        .path("resourceId");

    // 3. Create Savings Account Application
    Map<String, Object> savingsBody = new HashMap<>();
    savingsBody.put("clientId", clientId);
    savingsBody.put("productId", productId);
    savingsBody.put("locale", "en");
    savingsBody.put("dateFormat", "dd MMMM yyyy");
    savingsBody.put("submittedOnDate", "01 January 2026");

    Integer accountId = given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "mifos", "password"))
        .body(savingsBody)
        .post(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/savingsaccounts")
        .then()
        .statusCode(200)
        .extract()
        .path("savingsId");

    // 4. Create Self Service User (via JDBC directly to bypass email constraints/setup if any)
    String selfUser = "user_" + clientName;
    Properties props = new Properties();
    props.setProperty("user", "postgres");
    props.setProperty("password", "postgres");

    // Get the Fineract roleId for 'Self Service User'
    Integer roleId = given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "mifos", "password"))
        .get(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/roles")
        .jsonPath().getInt("find { it.name == 'Self Service User' }.id");

    try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), props)) {
        try (Statement st = conn.createStatement()) {
            String insertUser = "INSERT INTO m_appuser(office_id, username, password, email, firstname, lastname, is_deleted, nonexpired, nonlocked, nonexpired_credentials, enabled, firsttime_login_remaining) " +
                                "VALUES (1, '" + selfUser + "', (SELECT password FROM m_appuser WHERE username='mifos' LIMIT 1), '" + selfUser + "@fineract.org', 'Tomas', 'Test', false, true, true, true, true, false) RETURNING id";
            ResultSet rs = st.executeQuery(insertUser);
            rs.next();
            long newUserId = rs.getLong(1);
            
            st.execute("INSERT INTO m_appuser_role(appuser_id, role_id) VALUES (" + newUserId + ", " + roleId + ")");
            st.execute("INSERT INTO m_appselfservice_user(id, office_id, username, password, email, firstname, lastname, nonexpired, nonlocked, nonexpired_credentials, enabled, firsttime_login_remaining) " +
                       "SELECT id, office_id, username, password, email, firstname, lastname, nonexpired, nonlocked, nonexpired_credentials, enabled, firsttime_login_remaining FROM m_appuser WHERE id = " + newUserId);
            st.execute("INSERT INTO m_appselfservice_user_role(appuser_id, role_id) VALUES (" + newUserId + ", " + roleId + ")");
            st.execute("INSERT INTO m_selfservice_user_client_mapping(appuser_id, client_id) VALUES (" + newUserId + ", " + clientId + ")");
        }
    }

    // 5. Test Serialization Logic!
    Response response = given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), selfUser, "password"))
        .queryParam("fields", "savingsAccounts")
        .when()
        .get(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/self/clients/" + clientId + "/accounts")
        .then()
        .statusCode(200)
        .extract()
        .response();

    // Inclusion Proof: Verify the savings accounts array is fetched AND its elements are populated
    String accountNo = response.jsonPath().getString("savingsAccounts[0].accountNo");
    assertThat(accountNo).isNotNull();
    
    // Exclusion Proof: Verify other wrappers (e.g. loanAccounts or shareAccounts) are stripped entirely 
    // because we requested `fields=savingsAccounts`
    assertThat((Object) response.jsonPath().get("loanAccounts")).isNull();
    assertThat((Object) response.jsonPath().get("shareAccounts")).isNull();
  }
}
