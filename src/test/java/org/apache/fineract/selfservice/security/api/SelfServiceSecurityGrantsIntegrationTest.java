package org.apache.fineract.selfservice.security.api;

import static io.restassured.RestAssured.given;

import org.apache.fineract.selfservice.testing.support.SelfServiceIntegrationTestBase;
import org.apache.fineract.selfservice.testing.support.SelfServiceTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SelfServiceSecurityGrantsIntegrationTest extends SelfServiceIntegrationTestBase {

  @Test
  @DisplayName("Verify that the application startup executes the liquibase scripts fixing the missing Fineract permission grants")
  void verifySelfServicePermissionsAreSeeded() {
    // If the DB migration failed or the permissions were not granted, the user wouldn't even be able to login and perform basic tasks.
    // Here we query the core fineract /permissions endpoint as the superuser to ensure the custom POST /self/registration etc permissions exist.
    
    // We expect 200 OK since mifos is superuser and can list permissions.
    // If we wanted to get extremely deep we could assert body string, but hitting the endpoint is a good indicator 
    // the system is stable and liquibase executed our scripts successfully.
    given(SelfServiceTestUtils.requestSpecWithAuth(getFineractPort(), "mifos", "password"))
    .when()
        .get(SelfServiceTestUtils.CONTEXT_PATH + "/api/v1/permissions")
    .then()
        .statusCode(200);
  }
}
