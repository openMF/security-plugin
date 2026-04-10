/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.fineract.selfservice.registration.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.selfservice.registration.domain.SelfServiceRegistration;
import org.springframework.lang.NonNull;

/**
 * JPA persistence entity for self-service user registration requests.
 *
 * <p>All {@code jakarta.persistence.*} annotations live exclusively here. Domain logic belongs in
 * {@link SelfServiceRegistration}.
 */
@Entity
@Table(name = "request_audit_table")
public class SelfServiceRegistrationJpaEntity extends AbstractPersistableCustom<Long> {

  @ManyToOne
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;

  @Column(name = "account_number", length = 100, nullable = false)
  private String accountNumber;

  @Column(name = "firstname", length = 100, nullable = false)
  private String firstName;

  @Column(name = "middlename", length = 100)
  private String middleName;

  @Column(name = "lastname", length = 100, nullable = false)
  private String lastName;

  @Column(name = "mobile_number", length = 50)
  private String mobileNumber;

  @Column(name = "email", length = 100, nullable = false)
  private String email;

  @Column(name = "authentication_token", length = 100)
  private String authenticationToken;

  @Column(name = "username", length = 100, nullable = false)
  private String username;

  @Column(name = "password", length = 100, nullable = false)
  private String password;

  @Column(name = "created_date", nullable = false)
  private LocalDateTime createdDate;

  /** Required by JPA. */
  protected SelfServiceRegistrationJpaEntity() {}

  private SelfServiceRegistrationJpaEntity(
      Client client,
      String accountNumber,
      String firstName,
      String middleName,
      String lastName,
      String mobileNumber,
      String email,
      String authenticationToken,
      String username,
      String password,
      LocalDateTime createdDate) {
    this.client = client;
    this.accountNumber = accountNumber;
    this.firstName = firstName;
    this.middleName = middleName;
    this.lastName = lastName;
    this.mobileNumber = mobileNumber;
    this.email = email;
    this.authenticationToken = authenticationToken;
    this.username = username;
    this.password = password;
    this.createdDate = createdDate;
  }

  /**
   * Creates a new (not-yet-persisted) JPA entity from the domain object.
   *
   * @param domain the domain registration request
   * @param client the resolved {@link Client} JPA entity (fetched by the adapter using
   *     {@code domain.getClientId()})
   */
  @NonNull
  public static SelfServiceRegistrationJpaEntity fromNewDomain(
      SelfServiceRegistration domain, Client client) {
    return new SelfServiceRegistrationJpaEntity(
        client,
        domain.getAccountNumber(),
        domain.getFirstName(),
        domain.getMiddleName(),
        domain.getLastName(),
        domain.getMobileNumber(),
        domain.getEmail(),
        domain.getAuthenticationToken(),
        domain.getUsername(),
        domain.getPassword(),
        domain.getCreatedDate());
  }

  /** Converts this JPA entity back to the pure domain object. */
  public SelfServiceRegistration toDomain() {
    return SelfServiceRegistration.reconstruct(
        getId(),
        client != null ? client.getId() : null,
        accountNumber,
        firstName,
        middleName,
        lastName,
        mobileNumber,
        email,
        authenticationToken,
        username,
        password,
        createdDate);
  }
  public void updateFromDomain(SelfServiceRegistration domain, Client client) {
    this.client = client;
    this.accountNumber = domain.getAccountNumber();
    this.firstName = domain.getFirstName();
    this.middleName = domain.getMiddleName();
    this.lastName = domain.getLastName();
    this.mobileNumber = domain.getMobileNumber();
    this.email = domain.getEmail();
    this.authenticationToken = domain.getAuthenticationToken();
    this.username = domain.getUsername();
    this.password = domain.getPassword();
    this.createdDate = domain.getCreatedDate();
  }
}
