package org.apache.fineract.selfservice.registration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class SelfServiceAuthorizationTokenServiceTest {

    @Mock private Environment env;

    @Test
    void generateToken_defaultsToUuidV7() {
        SelfServiceAuthorizationTokenService service = new SelfServiceAuthorizationTokenService(env);

        String token = service.generateToken();

        assertTrue(token.matches("^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"));
    }

    @Test
    void generateToken_respectsNumericConfiguration() {
        when(env.getProperty("mifos.self.service.token.type")).thenReturn("numeric");
        SelfServiceAuthorizationTokenService service = new SelfServiceAuthorizationTokenService(env);

        String token = service.generateToken();

        assertTrue(token.matches("\\d{6}"));
    }

    @Test
    void calculateExpiry_usesConfiguredSeconds() {
        when(env.getProperty("mifos.self.service.token.expiry.time", Integer.class, 30)).thenReturn(45);
        SelfServiceAuthorizationTokenService service = new SelfServiceAuthorizationTokenService(env);
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 13, 10, 0, 0);

        LocalDateTime expiresAt = service.calculateExpiry(createdAt);

        assertEquals(createdAt.plusSeconds(45), expiresAt);
    }

    @Test
    void calculateExpiry_defaultsToThirtySeconds() {
        when(env.getProperty("mifos.self.service.token.expiry.time", Integer.class, 30)).thenReturn(30);
        SelfServiceAuthorizationTokenService service = new SelfServiceAuthorizationTokenService(env);
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 13, 10, 0, 0);

        LocalDateTime expiresAt = service.calculateExpiry(createdAt);

        assertEquals(createdAt.plusSeconds(30), expiresAt);
    }
}
