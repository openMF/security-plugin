package org.apache.fineract.selfservice.registration.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.selfservice.registration.domain.SelfServiceRegistrationRepository;
import org.apache.fineract.selfservice.useradministration.service.AppSelfServiceUserReadPlatformService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SelfServiceRegistrationWritePlatformServiceImplTest {

  @Mock private SelfServiceRegistrationRepository selfServiceRegistrationRepository;
  @Mock private AppSelfServiceUserReadPlatformService appUserReadPlatformService;

  @InjectMocks private SelfServiceRegistrationWritePlatformServiceImpl service;

  @Test
  void randomAuthorizationTokenGeneration_shouldReturn4DigitString() {
    String token =
        SelfServiceRegistrationWritePlatformServiceImpl.randomAuthorizationTokenGeneration();
    assertNotNull(token);
    int value = Integer.parseInt(token);
    assertTrue(
        value >= 1000 && value <= 9999, "Token should be a 4-digit number but was: " + token);
  }

  @Test
  void randomAuthorizationTokenGeneration_shouldProduceDifferentTokens() {
    // Not guaranteed, but statistically near-certain with 9000 possible values
    java.util.Set<String> tokens = new java.util.HashSet<>();
    for (int i = 0; i < 20; i++) {
      tokens.add(
          SelfServiceRegistrationWritePlatformServiceImpl.randomAuthorizationTokenGeneration());
    }
    assertTrue(tokens.size() > 1, "Should generate different tokens across multiple calls");
  }

  @Test
  void validateForDuplicateUsername_shouldThrowWhenUsernameExists() {
    when(appUserReadPlatformService.isUsernameExist("existing.user")).thenReturn(true);

    PlatformDataIntegrityException ex =
        assertThrows(
            PlatformDataIntegrityException.class,
            () -> service.validateForDuplicateUsername("existing.user"));

    assertTrue(ex.getGlobalisationMessageCode().contains("duplicate.username"));
  }

  @Test
  void validateForDuplicateUsername_shouldPassWhenUsernameIsNew() {
    when(appUserReadPlatformService.isUsernameExist("new.user")).thenReturn(false);

    assertDoesNotThrow(() -> service.validateForDuplicateUsername("new.user"));
  }
}
