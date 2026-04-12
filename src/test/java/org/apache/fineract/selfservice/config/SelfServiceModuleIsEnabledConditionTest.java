/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.selfservice.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.junit.jupiter.api.Test;

class SelfServiceModuleIsEnabledConditionTest {

  @Test
  void matches_shouldAlwaysReturnTrueRegardlessOfProperties() {
    SelfServiceModuleIsEnabledCondition condition = new SelfServiceModuleIsEnabledCondition();
    FineractProperties properties = mock(FineractProperties.class);
    // The matches method always returns true — the module is always enabled when loaded.
    // This documents that there is currently no configuration toggle for the module.
    assertTrue(condition.matches(properties));
  }

  @Test
  void matches_shouldReturnTrueEvenWithNullProperties() {
    SelfServiceModuleIsEnabledCondition condition = new SelfServiceModuleIsEnabledCondition();
    assertTrue(condition.matches(null));
  }
}
