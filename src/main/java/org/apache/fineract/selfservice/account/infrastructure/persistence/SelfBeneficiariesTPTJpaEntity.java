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
package org.apache.fineract.selfservice.account.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.selfservice.account.domain.SelfBeneficiariesTPT;

/**
 * JPA persistence entity for self-service third-party transfer beneficiaries.
 *
 * <p>All {@code jakarta.persistence.*} annotations live exclusively here. Domain logic belongs in
 * {@link SelfBeneficiariesTPT}.
 */
@Entity
@Table(
    name = "m_selfservice_beneficiaries_tpt",
    uniqueConstraints = {
      @UniqueConstraint(
          columnNames = {"name", "app_user_id", "is_active"},
          name = "name")
    })
public class SelfBeneficiariesTPTJpaEntity extends AbstractPersistableCustom<Long> {

  @Column(name = "app_user_id", nullable = false)
  private Long appUserId;

  @Column(name = "name", length = 50, nullable = false)
  private String name;

  @Column(name = "office_id", nullable = false)
  private Long officeId;

  @Column(name = "client_id", nullable = false)
  private Long clientId;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(name = "account_type", nullable = false)
  private Integer accountType;

  @Column(name = "transfer_limit")
  private Long transferLimit;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  /** Required by JPA. */
  protected SelfBeneficiariesTPTJpaEntity() {}

  private SelfBeneficiariesTPTJpaEntity(
      Long appUserId,
      String name,
      Long officeId,
      Long clientId,
      Long accountId,
      Integer accountType,
      Long transferLimit,
      boolean isActive) {
    this.appUserId = appUserId;
    this.name = name;
    this.officeId = officeId;
    this.clientId = clientId;
    this.accountId = accountId;
    this.accountType = accountType;
    this.transferLimit = transferLimit;
    this.isActive = isActive;
  }

  /** Creates a new (not-yet-persisted) JPA entity from the domain object. */
  public static SelfBeneficiariesTPTJpaEntity fromNewDomain(SelfBeneficiariesTPT domain) {
    return new SelfBeneficiariesTPTJpaEntity(
        domain.getAppUserId(),
        domain.getName(),
        domain.getOfficeId(),
        domain.getClientId(),
        domain.getAccountId(),
        domain.getAccountType(),
        domain.getTransferLimit(),
        domain.isActive());
  }

  /** Applies mutable field values from the domain object onto this (already-managed) JPA entity. */
  public void applyChangesFrom(SelfBeneficiariesTPT domain) {
    this.name = domain.getName();
    this.transferLimit = domain.getTransferLimit();
    this.isActive = domain.isActive();
  }

  /** Converts this JPA entity back to the pure domain object. */
  public SelfBeneficiariesTPT toDomain() {
    return new SelfBeneficiariesTPT(
        getId(),
        appUserId,
        name,
        officeId,
        clientId,
        accountId,
        accountType,
        transferLimit,
        isActive);
  }
}
