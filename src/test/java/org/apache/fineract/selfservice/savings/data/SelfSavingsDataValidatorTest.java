/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.selfservice.savings.data;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SelfSavingsDataValidatorTest {

  private SelfSavingsDataValidator validator;

  @BeforeEach
  void setUp() {
    FromJsonHelper fromJsonHelper = new FromJsonHelper();
    validator = new SelfSavingsDataValidator(fromJsonHelper);
  }

  @Test
  void validateSavingsApplication_shouldThrowOnBlankJson() {
    assertThrows(InvalidJsonException.class, () -> validator.validateSavingsApplication(""));
    assertThrows(InvalidJsonException.class, () -> validator.validateSavingsApplication("   "));
  }

  @Test
  void validateSavingsApplication_shouldThrowOnMissingClientId() {
    String json = "{\"productId\": 1}";
    assertThrows(
        PlatformApiDataValidationException.class, () -> validator.validateSavingsApplication(json));
  }

  @Test
  void validateSavingsApplication_shouldThrowOnNullClientId() {
    String json = "{\"clientId\": null}";
    assertThrows(
        PlatformApiDataValidationException.class, () -> validator.validateSavingsApplication(json));
  }

  @Test
  void validateSavingsApplication_shouldReturnClientIdOnValidInput() {
    String json = "{\"clientId\": 42}";
    HashMap<String, Object> result = validator.validateSavingsApplication(json);
    assertEquals(42L, result.get("clientId"));
  }

  @Test
  void validateSavingsApplication_shouldAcceptClientIdWithOtherFields() {
    String json = "{\"clientId\": 10, \"productId\": 1, \"locale\": \"en\"}";
    HashMap<String, Object> result = validator.validateSavingsApplication(json);
    assertEquals(10L, result.get("clientId"));
  }
}
