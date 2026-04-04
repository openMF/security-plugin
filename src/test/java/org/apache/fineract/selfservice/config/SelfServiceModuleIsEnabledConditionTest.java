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
