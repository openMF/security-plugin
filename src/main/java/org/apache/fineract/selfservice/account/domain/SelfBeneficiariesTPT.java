/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.fineract.selfservice.account.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Pure domain entity for a self-service third-party transfer beneficiary.
 *
 * <p>No JPA or framework annotations are permitted here. All persistence mapping lives in
 * {@code account.infrastructure.persistence.SelfBeneficiariesTPTJpaEntity}.
 */
public class SelfBeneficiariesTPT {

  /** Database-generated surrogate key. {@code null} for instances that have not yet been saved. */
  private Long id;

  private final Long appUserId;
  private String name;
  private final Long officeId;
  private final Long clientId;
  private final Long accountId;
  private final Integer accountType;
  private Long transferLimit;
  private boolean isActive;


  /** Constructor for creating a new (not-yet-persisted) beneficiary. */
  public SelfBeneficiariesTPT(
      Long appUserId,
      String name,
      Long officeId,
      Long clientId,
      Long accountId,
      Integer accountType,
      Long transferLimit) {
    Objects.requireNonNull(appUserId, "appUserId cannot be null");
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(officeId, "officeId cannot be null");
    Objects.requireNonNull(clientId, "clientId cannot be null");
    Objects.requireNonNull(accountId, "accountId cannot be null");
    Objects.requireNonNull(accountType, "accountType cannot be null");
    this.appUserId = appUserId;
    this.name = name;
    this.officeId = officeId;
    this.clientId = clientId;
    this.accountId = accountId;
    this.accountType = accountType;
    this.transferLimit = transferLimit;
    this.isActive = true;
  }

  /**
   * Reconstruction constructor used by the infrastructure adapter to hydrate a persisted entity.
   * Must not be called directly from application or domain code.
   */
  public SelfBeneficiariesTPT(
      Long id,
      Long appUserId,
      String name,
      Long officeId,
      Long clientId,
      Long accountId,
      Integer accountType,
      Long transferLimit,
      boolean isActive) {
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(appUserId, "appUserId cannot be null");
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(officeId, "officeId cannot be null");
    Objects.requireNonNull(clientId, "clientId cannot be null");
    Objects.requireNonNull(accountId, "accountId cannot be null");
    Objects.requireNonNull(accountType, "accountType cannot be null");
    this.id = id;
    this.appUserId = appUserId;
    this.name = name;
    this.officeId = officeId;
    this.clientId = clientId;
    this.accountId = accountId;
    this.accountType = accountType;
    this.transferLimit = transferLimit;
    this.isActive = isActive;
  }

  /**
   * Returns the surrogate key of this beneficiary, or {@code null} if not yet persisted.
   *
   * @return the database-generated identifier of this {@link SelfBeneficiariesTPT} entity
   */
  public Long getId() {
    return id;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    Objects.requireNonNull(name, "name cannot be null");
    this.name = name;
  }

  public Long getTransferLimit() {
    return this.transferLimit;
  }

  public void setTransferLimit(Long transferLimit) {
    this.transferLimit = transferLimit;
  }

  public boolean isActive() {
    return this.isActive;
  }

  public void setActive(boolean isActive) {
    this.isActive = isActive;
  }

  public Long getAppUserId() {
    return this.appUserId;
  }

  public Long getOfficeId() {
    return this.officeId;
  }

  public Long getClientId() {
    return this.clientId;
  }

  public Long getAccountId() {
    return this.accountId;
  }

  public Integer getAccountType() {
    return this.accountType;
  }

  /** Applies the requested updates and returns a map of the fields that actually changed. */
  public Map<String, Object> update(
      boolean hasName, String newName, boolean hasTransferLimit, Long newTransferLimit) {
    Map<String, Object> changes = new HashMap<>();
    if (hasName) {
      Objects.requireNonNull(newName, "name cannot be null");
      if (!Objects.equals(this.name, newName)) {
        this.name = newName;
        changes.put("name", newName);
      }
    }
    if (hasTransferLimit && !Objects.equals(this.transferLimit, newTransferLimit)) {
      this.transferLimit = newTransferLimit;
      changes.put("transferLimit", newTransferLimit);
    }
    return changes;
  }
}
