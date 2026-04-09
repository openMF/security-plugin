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
package org.apache.fineract.selfservice.registration.domain;

import java.time.LocalDateTime;

/**
 * Pure domain object representing a self-service user registration request.
 *
 * <p>No JPA or framework annotations are permitted here. The client relationship is intentionally
 * held as a surrogate key ({@code clientId}) rather than an object reference, keeping the domain
 * free of infrastructure types. All persistence mapping lives in
 * {@code registration.infrastructure.persistence.SelfServiceRegistrationJpaEntity}.
 */
public class SelfServiceRegistration {

  /** Database-generated surrogate key. {@code null} for instances not yet saved. */
  private Long id;

  private final Long clientId;
  private final String accountNumber;
  private final String firstName;
  private final String middleName;
  private final String lastName;
  private final String mobileNumber;
  private final String email;
  private final String authenticationToken;
  private final String username;
  private final String password;
  private final LocalDateTime createdDate;

  protected SelfServiceRegistration() {
    this.clientId = null;
    this.accountNumber = null;
    this.firstName = null;
    this.middleName = null;
    this.lastName = null;
    this.mobileNumber = null;
    this.email = null;
    this.authenticationToken = null;
    this.username = null;
    this.password = null;
    this.createdDate = null;
  }

  /** Constructor for creating a new (not-yet-persisted) registration request. */
  public SelfServiceRegistration(
      final Long clientId,
      final String accountNumber,
      final String firstName,
      final String middleName,
      final String lastName,
      final String mobileNumber,
      final String email,
      final String authenticationToken,
      final String username,
      final String password,
      final LocalDateTime createdDate) {
    this.clientId = clientId;
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
   * Reconstruction constructor used by the infrastructure adapter to hydrate a persisted entity.
   * Must not be called directly from application or domain code.
   */
  public SelfServiceRegistration(
      final Long id,
      final Long clientId,
      final String accountNumber,
      final String firstName,
      final String middleName,
      final String lastName,
      final String mobileNumber,
      final String email,
      final String authenticationToken,
      final String username,
      final String password,
      final LocalDateTime createdDate) {
    this.id = id;
    this.clientId = clientId;
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
   * Factory method that creates a new registration request. The caller is responsible for
   * supplying the resolved {@code clientId}; the domain layer does not fetch clients itself.
   */
  public static SelfServiceRegistration instance(
      final Long clientId,
      final String accountNumber,
      final String firstName,
      final String middleName,
      final String lastName,
      final String mobileNumber,
      final String email,
      final String authenticationToken,
      final String username,
      final String password,
      final LocalDateTime createdDate) {
    return new SelfServiceRegistration(
        clientId, accountNumber, firstName, middleName, lastName,
        mobileNumber, email, authenticationToken, username, password, createdDate);
  }

  public Long getId() {
    return id;
  }

  /** Returns the surrogate key of the associated client. */
  public Long getClientId() {
    return this.clientId;
  }

  public String getFirstName() {
    return this.firstName;
  }

  public String getMiddleName() {
    return this.middleName;
  }

  public String getLastName() {
    return this.lastName;
  }

  public String getMobileNumber() {
    return this.mobileNumber;
  }

  public String getEmail() {
    return this.email;
  }

  public String getAuthenticationToken() {
    return this.authenticationToken;
  }

  public LocalDateTime getCreatedDate() {
    return this.createdDate;
  }

  public String getUsername() {
    return this.username;
  }

  public String getPassword() {
    return this.password;
  }

  public String getAccountNumber() {
    return this.accountNumber;
  }
}
