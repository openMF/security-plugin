package org.apache.fineract.selfservice.shareaccounts.data;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SelfShareAccountsDataValidatorTest {

  private SelfShareAccountsDataValidator validator;

  @BeforeEach
  void setUp() {
    FromJsonHelper fromJsonHelper = new FromJsonHelper();
    validator = new SelfShareAccountsDataValidator(fromJsonHelper);
  }

  @Test
  void validateShareAccountApplication_shouldThrowOnBlankJson() {
    assertThrows(InvalidJsonException.class, () -> validator.validateShareAccountApplication(""));
    assertThrows(
        InvalidJsonException.class, () -> validator.validateShareAccountApplication("   "));
  }

  @Test
  void validateShareAccountApplication_shouldThrowOnMissingClientId() {
    String json = "{\"productId\": 1}";
    assertThrows(
        PlatformApiDataValidationException.class,
        () -> validator.validateShareAccountApplication(json));
  }

  @Test
  void validateShareAccountApplication_shouldReturnClientIdOnValidInput() {
    String json = "{\"clientId\": \"15\"}";
    HashMap<String, Object> result = validator.validateShareAccountApplication(json);
    assertEquals(15L, result.get("clientId"));
  }
}
