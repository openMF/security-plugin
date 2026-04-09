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

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.selfservice.account.domain.SelfBeneficiariesTPT;
import org.apache.fineract.selfservice.account.domain.SelfBeneficiariesTPTRepository;

/**
 * Adapts {@link SelfBeneficiariesTPTJpaRepository} (Spring Data / JPA) to the pure domain
 * interface {@link SelfBeneficiariesTPTRepository}.
 *
 * <p>All mapping between the domain object and the JPA entity is encapsulated here, keeping both
 * the domain layer and the service layer free of persistence concerns.
 */
@RequiredArgsConstructor
public class SelfBeneficiariesTPTRepositoryAdapter implements SelfBeneficiariesTPTRepository {

  private final SelfBeneficiariesTPTJpaRepository jpaRepository;

  @Override
  public SelfBeneficiariesTPT save(SelfBeneficiariesTPT domain) {
    return persist(domain, false);
  }

  @Override
  public SelfBeneficiariesTPT saveAndFlush(SelfBeneficiariesTPT domain) {
    return persist(domain, true);
  }

  @Override
  public Optional<SelfBeneficiariesTPT> findById(Long id) {
    return jpaRepository.findById(id).map(SelfBeneficiariesTPTJpaEntity::toDomain);
  }

  // ── private helpers ────────────────────────────────────────────────────────

  private SelfBeneficiariesTPT persist(SelfBeneficiariesTPT domain, boolean flush) {
    SelfBeneficiariesTPTJpaEntity entity;

    if (domain.getId() == null) {
      // New entity: create from scratch
      entity = SelfBeneficiariesTPTJpaEntity.fromNewDomain(domain);
    } else {
      // Existing entity: load the managed JPA instance and apply mutations.
      // This avoids detached-entity issues and ensures Hibernate tracks changes correctly.
      entity =
          jpaRepository
              .findById(domain.getId())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "SelfBeneficiariesTPT not found with id " + domain.getId()));
      entity.applyChangesFrom(domain);
    }

    SelfBeneficiariesTPTJpaEntity saved =
        flush ? jpaRepository.saveAndFlush(entity) : jpaRepository.save(entity);
    return saved.toDomain();
  }
}
