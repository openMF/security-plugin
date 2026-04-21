/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.selfservice.registration.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Pure domain object representing a self-service user registration request.
 *
 * <p>No JPA or framework annotations are permitted here. The client relationship is intentionally
 * held as a surrogate key ({@code clientId}) rather than an object reference, keeping the domain
 * free of infrastructure types. All persistence mapping lives in
 * {@code registration.infrastructure.persistence.SelfServiceRegistrationJpaEntity}.
 */
public class SelfServiceRegistration {

    /**
     * Sentinel value stored in the {@code password} column when a registration record is used for
     * a password-reset flow rather than an initial enrolment.
     */
    public static final String PASSWORD_RESET_SENTINEL = "<PASSWORD_RESET>";

    /** Database-generated surrogate key. {@code null} for instances not yet saved. */
    private Long id;

    /** Surrogate key of the associated {@code Client} record. */
    private final Long clientId;

    private final String accountNumber;
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final String mobileNumber;
    private final String email;
    private final String authenticationToken;

    /**
     * External (UUID-style) authorization token used by the client-user and password-reset flows.
     * Maps to the {@code external_authorization_token} column.
     */
    private final String externalAuthorizationToken;

    private final String username;
    private final String password;
    private final LocalDateTime createdDate;
    private final LocalDateTime expiresAt;
    private final SelfServiceRequestType requestType;

    /**
     * Mutable flag: set to {@code true} once the registration/password-reset token has been used.
     */
    private boolean consumed;

    // ------------------------------------
    // Private constructors
    // ------------------------------------

    /**
     * Full constructor used by all factory methods.
     */
    private SelfServiceRegistration(
            final Long id,
            final Long clientId,
            final String accountNumber,
            final String firstName,
            final String middleName,
            final String lastName,
            final String mobileNumber,
            final String email,
            final String authenticationToken,
            final String externalAuthorizationToken,
            final String username,
            final String password,
            final LocalDateTime createdDate,
            final LocalDateTime expiresAt,
            final SelfServiceRequestType requestType,
            final boolean consumed) {
        this.id = id;
        this.clientId = clientId;
        this.accountNumber = accountNumber;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.mobileNumber = mobileNumber;
        this.email = email;
        this.authenticationToken = authenticationToken;
        this.externalAuthorizationToken = externalAuthorizationToken;
        this.username = username;
        this.password = password;
        this.createdDate = createdDate;
        this.expiresAt = expiresAt;
        this.requestType = requestType;
        this.consumed = consumed;
    }

    // ------------------------------------
    // Factory methods
    // ------------------------------------

    /**
     * Creates a new (not-yet-persisted) standard registration request.
     *
     * <p>The caller is responsible for supplying the resolved {@code clientId}; the domain layer
     * does not fetch clients itself.
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
        Objects.requireNonNull(clientId, "clientId cannot be null");
        Objects.requireNonNull(createdDate, "createdDate cannot be null");
        return new SelfServiceRegistration(
                null, clientId, accountNumber, firstName, middleName, lastName, mobileNumber,
                email, authenticationToken, authenticationToken, username, password,
                createdDate, null, SelfServiceRequestType.REGISTRATION, false);
    }

    /**
     * Creates a new (not-yet-persisted) registration request for a specified request type (e.g.
     * client-user enrolment or password-reset).
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
            final String externalAuthorizationToken,
            final String username,
            final String password,
            final SelfServiceRequestType requestType,
            final LocalDateTime createdDate,
            final LocalDateTime expiresAt) {
        Objects.requireNonNull(clientId, "clientId cannot be null");
        Objects.requireNonNull(createdDate, "createdDate cannot be null");
        Objects.requireNonNull(requestType, "requestType cannot be null");
        return new SelfServiceRegistration(
                null, clientId, accountNumber, firstName, middleName, lastName, mobileNumber,
                email, authenticationToken, externalAuthorizationToken, username, password,
                createdDate, expiresAt, requestType, false);
    }

    /**
     * Factory method used by the infrastructure adapter to hydrate a persisted entity back into a
     * domain object. Must not be called directly from application or domain code.
     */
    public static SelfServiceRegistration reconstruct(
            final Long id,
            final Long clientId,
            final String accountNumber,
            final String firstName,
            final String middleName,
            final String lastName,
            final String mobileNumber,
            final String email,
            final String authenticationToken,
            final String externalAuthorizationToken,
            final String username,
            final String password,
            final LocalDateTime createdDate,
            final LocalDateTime expiresAt,
            final SelfServiceRequestType requestType,
            final boolean consumed) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(clientId, "clientId cannot be null");
        Objects.requireNonNull(createdDate, "createdDate cannot be null");
        return new SelfServiceRegistration(
                id, clientId, accountNumber, firstName, middleName, lastName, mobileNumber,
                email, authenticationToken, externalAuthorizationToken, username, password,
                createdDate, expiresAt, requestType, consumed);
    }

    // ------------------------------------
    // Accessors
    // ------------------------------------

    public Long getId() {
        return id;
    }

    /** Returns the surrogate key of the associated client record. */
    public Long getClientId() {
        return this.clientId;
    }

    public String getAccountNumber() {
        return this.accountNumber;
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

    /**
     * Returns the external (UUID-style) authorization token.
     *
     * @deprecated Prefer {@link #getExternalAuthorizationToken()} to match the persisted column
     *     name.
     */
    @Deprecated(forRemoval = false)
    public String getExternalAuthenticationToken() {
        return getExternalAuthorizationToken();
    }

    public String getExternalAuthorizationToken() {
        return this.externalAuthorizationToken;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public LocalDateTime getCreatedDate() {
        return this.createdDate;
    }

    public LocalDateTime getExpiresAt() {
        return this.expiresAt;
    }

    public SelfServiceRequestType getRequestType() {
        return this.requestType;
    }

    public boolean isConsumed() {
        return this.consumed;
    }

    // ------------------------------------
    // Domain behaviour
    // ------------------------------------

    /**
     * Returns {@code true} if this registration record has passed its expiry time.
     *
     * @param now the current instant to compare against; must not be {@code null}
     */
    public boolean isExpired(final LocalDateTime now) {
        Objects.requireNonNull(now, "now must not be null");
        return this.expiresAt != null && !now.isBefore(this.expiresAt);
    }

    /** Marks this record as consumed, preventing it from being used again. */
    public void markConsumed() {
        this.consumed = true;
    }
}
