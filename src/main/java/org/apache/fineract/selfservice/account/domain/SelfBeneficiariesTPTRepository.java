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

import java.util.Optional;

/**
 * Domain repository contract for {@link SelfBeneficiariesTPT}.
 *
 * <p>This interface is deliberately free of any Spring or JPA dependency. The implementation lives
 * in {@code account.infrastructure.persistence.SelfBeneficiariesTPTRepositoryAdapter}.
 */
public interface SelfBeneficiariesTPTRepository {

  /**
   * Persists a new beneficiary or merges an existing one.
   *
   * @return the saved domain object (with {@code id} populated for new entities)
   */
  SelfBeneficiariesTPT save(SelfBeneficiariesTPT entity);

  /**
   * Persists and immediately flushes to the underlying store.
   *
   * @return the saved domain object (with {@code id} populated for new entities)
   */
  SelfBeneficiariesTPT saveAndFlush(SelfBeneficiariesTPT entity);

  /** Looks up a beneficiary by its surrogate key. */
  Optional<SelfBeneficiariesTPT> findById(Long id);
}
