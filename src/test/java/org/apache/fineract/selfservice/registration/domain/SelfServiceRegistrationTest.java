package org.apache.fineract.selfservice.registration.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.apache.fineract.portfolio.client.domain.Client;
import org.junit.jupiter.api.Test;

class SelfServiceRegistrationTest {

  @Test
  void instance_shouldCreateRegistrationWithAllFields() {
    SelfServiceRegistration reg =
        SelfServiceRegistration.instance(
            1L,
            "000000001",
            "Pedro",
            "Marmol",
            "Perez",
            "5522649498",
            "pedro@test.com",
            "1234",
            "pedro.marmol",
            "SecurePass123#",
            java.time.LocalDateTime.now());

    assertEquals(1L, reg.getClientId());
    assertEquals("000000001", reg.getAccountNumber());
    assertEquals("Pedro", reg.getFirstName());
    assertEquals("Marmol", reg.getMiddleName());
    assertEquals("Perez", reg.getLastName());
    assertEquals("5522649498", reg.getMobileNumber());
    assertEquals("pedro@test.com", reg.getEmail());
    assertEquals("1234", reg.getAuthenticationToken());
    assertEquals("pedro.marmol", reg.getUsername());
    assertEquals("SecurePass123#", reg.getPassword());
    assertNotNull(reg.getCreatedDate());
  }

  @Test
  void instance_shouldHandleNullMiddleName() {
    SelfServiceRegistration reg =
        SelfServiceRegistration.instance(
            1L,
            "000000002",
            "John",
            null,
            "Doe",
            null,
            "john@test.com",
            "5678",
            "john.doe",
            "Pass456#",
            java.time.LocalDateTime.now());

    assertNull(reg.getMiddleName());
    assertNull(reg.getMobileNumber());
    assertEquals("John", reg.getFirstName());
  }

  @Test
  void defaultConstructor_shouldCreateEmptyInstance() {
    SelfServiceRegistration reg = new SelfServiceRegistration();
    assertNull(reg.getClientId());
    assertNull(reg.getFirstName());
  }
}
