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

/**
 * Domain repository contract for {@link SelfServiceRegistration}.
 *
 * <p>This interface is deliberately free of any Spring or JPA dependency. The implementation lives
 * in {@code registration.infrastructure.persistence.SelfServiceRegistrationRepositoryAdapter}.
 */
public interface SelfServiceRegistrationRepository {

  /**
   * Persists a new registration request and immediately flushes to the underlying store.
   *
   * @return the saved domain object (with {@code id} populated)
   */
  SelfServiceRegistration saveAndFlush(SelfServiceRegistration entity);

  /**
   * Looks up a registration request by its id and one-time authentication token.
   *
   * @return the matching request, or {@code null} if none is found
   */
  SelfServiceRegistration getRequestByIdAndAuthenticationToken(
      Long id, String authenticationToken);
}
