package org.apache.fineract.selfservice.security.api;

import static io.restassured.RestAssured.given;
import io.restassured.response.Response;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
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
    regBody.put("password", "Password123!");
    regBody.put("email", username + "@fineract.org");
    regBody.put("authenticationMode", "email");

    // This will create a registration request but might fail sending the email. We just need the DB record.
    // However, if the email send fails, the transaction is rolled back!
    // So actually, let's mock or use the AppUser route... wait, if the transaction rolls back, we can't extract the token!
    // BUT we CAN just INSERT the AppSelfServiceUser directly using JDBC!

    java.util.Properties props = new java.util.Properties();
    props.setProperty("user", "postgres");
    props.setProperty("password", "postgres");

    String jdbcUrl = postgres.getJdbcUrl();

    try (java.sql.Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, props)) {
        // Find the generated hashed password from 'mifos' to copy it
        try (java.sql.Statement st = conn.createStatement()) {
            // Insert into m_appuser
            String insertUser = "INSERT INTO m_appuser(office_id, username, password, email, firstname, lastname, is_deleted, nonexpired, nonlocked, nonexpired_credentials, enabled, firsttime_login_remaining) " +
                                "VALUES (1, 'tomas', (SELECT password FROM m_appuser WHERE username='mifos' LIMIT 1), 'tomas@fineract.org', 'Tomas', 'Test', false, true, true, true, true, false) RETURNING id";
            
            java.sql.ResultSet rs = st.executeQuery(insertUser);
            rs.next();
            long newUserId = rs.getLong(1);
            
            // Map AppUser to Role
            st.execute("INSERT INTO m_appuser_role(appuser_id, role_id) VALUES (" + newUserId + ", " + roleId + ")");
            
            // Insert into plugin m_appselfservice_user
            st.execute("INSERT INTO m_appselfservice_user(id, office_id, username, password, email, firstname, lastname, nonexpired, nonlocked, nonexpired_credentials, enabled, firsttime_login_remaining) " +
                       "SELECT id, office_id, username, password, email, firstname, lastname, nonexpired, nonlocked, nonexpired_credentials, enabled, firsttime_login_remaining FROM m_appuser WHERE id = " + newUserId);
            
            // Map SelfService User to Role
            st.execute("INSERT INTO m_appselfservice_user_role(appuser_id, role_id) VALUES (" + newUserId + ", " + roleId + ")");
            
            // Map SelfService User to Client
            st.execute("INSERT INTO m_selfservice_user_client_mapping(appuser_id, client_id) VALUES (" + newUserId + ", " + clientId + ")");
        }
    }

    // 4. Test the API without the permission: Expect 403
    given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "tomas", "password"))
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
    given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "tomas", "password"))
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
    given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "tomas", "password"))
        .when()
        .get(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/self/savingsproducts")
        .then()
        .statusCode(403);
  }
}
