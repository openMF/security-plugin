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

import static org.apache.fineract.selfservice.account.api.SelfBeneficiariesTPTApiConstants.NAME_PARAM_NAME;
import static org.apache.fineract.selfservice.account.api.SelfBeneficiariesTPTApiConstants.TRANSFER_LIMIT_PARAM_NAME;

import java.util.HashMap;
import java.util.Map;

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

  protected SelfBeneficiariesTPT() {
    this.appUserId = null;
    this.officeId = null;
    this.clientId = null;
    this.accountId = null;
    this.accountType = null;
  }

  /** Constructor for creating a new (not-yet-persisted) beneficiary. */
  public SelfBeneficiariesTPT(
      Long appUserId,
      String name,
      Long officeId,
      Long clientId,
      Long accountId,
      Integer accountType,
      Long transferLimit) {
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

  public Long getId() {
    return id;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
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
  public Map<String, Object> update(String newName, Long newTransferLimit) {
    Map<String, Object> changes = new HashMap<>();
    if (!this.name.equals(newName)) {
      this.name = newName;
      changes.put(NAME_PARAM_NAME, newName);
    }
    if ((this.transferLimit != null && !this.transferLimit.equals(newTransferLimit))
        || (this.transferLimit == null && newTransferLimit != null)) {
      this.transferLimit = newTransferLimit;
      changes.put(TRANSFER_LIMIT_PARAM_NAME, newTransferLimit);
    }
    return changes;
  }
}
