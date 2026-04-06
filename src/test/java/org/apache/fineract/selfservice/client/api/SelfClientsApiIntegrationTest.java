package org.apache.fineract.selfservice.client.api;

import static io.restassured.RestAssured.given;

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
}
