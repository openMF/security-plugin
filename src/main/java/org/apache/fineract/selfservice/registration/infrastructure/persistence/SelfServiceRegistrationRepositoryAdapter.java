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

import lombok.RequiredArgsConstructor;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.selfservice.registration.domain.SelfServiceRegistration;
import org.apache.fineract.selfservice.registration.domain.SelfServiceRegistrationRepository;

/**
 * Adapts {@link SelfServiceRegistrationJpaRepository} (Spring Data / JPA) to the pure domain
 * interface {@link SelfServiceRegistrationRepository}.
 *
 * <p>The adapter is responsible for resolving the {@link Client} JPA entity by the
 * {@code clientId} stored in the domain object, keeping both the domain layer and the service
 * layer free of persistence concerns.
 */
@RequiredArgsConstructor
public class SelfServiceRegistrationRepositoryAdapter implements SelfServiceRegistrationRepository {

  private final SelfServiceRegistrationJpaRepository jpaRepository;
  private final ClientRepositoryWrapper clientRepository;

  @Override
  public SelfServiceRegistration saveAndFlush(SelfServiceRegistration domain) {
    Client client = clientRepository.findOneWithNotFoundDetection(domain.getClientId());
    SelfServiceRegistrationJpaEntity entity;
    if (domain.getId() != null) {
      entity = jpaRepository.findById(domain.getId())
          .orElseThrow(() -> new IllegalStateException("Entity not found for id: " + domain.getId()));
      entity.updateFromDomain(domain, client);
    } else {
      entity = SelfServiceRegistrationJpaEntity.fromNewDomain(domain, client);
    }
    return jpaRepository.saveAndFlush(entity).toDomain();
  }

  @Override
  public SelfServiceRegistration getRequestByIdAndAuthenticationToken(
      Long id, String authenticationToken) {
    SelfServiceRegistrationJpaEntity entity =
        jpaRepository.findByIdAndAuthenticationToken(id, authenticationToken);
    return entity == null ? null : entity.toDomain();
  }
}
