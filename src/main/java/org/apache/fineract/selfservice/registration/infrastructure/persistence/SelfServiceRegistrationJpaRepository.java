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

import org.apache.fineract.selfservice.registration.domain.SelfServiceRequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SelfServiceRegistrationJpaRepository
    extends JpaRepository<SelfServiceRegistrationJpaEntity, Long> {

  @Query(
      "select r from SelfServiceRegistrationJpaEntity r"
          + " where r.id = :id and r.authenticationToken = :authenticationToken")
  SelfServiceRegistrationJpaEntity findByIdAndAuthenticationToken(
      @Param("id") Long id, @Param("authenticationToken") String authenticationToken);

  @Query(
      "select r from SelfServiceRegistrationJpaEntity r"
          + " where r.id = :id and r.authenticationToken = :authenticationToken"
          + " and r.requestType = :requestType")
  SelfServiceRegistrationJpaEntity findByIdAndAuthenticationTokenAndRequestType(
      @Param("id") Long id,
      @Param("authenticationToken") String authenticationToken,
      @Param("requestType") SelfServiceRequestType requestType);

  @Query(
      "select r from SelfServiceRegistrationJpaEntity r"
          + " where r.externalAuthorizationToken = :token and r.requestType = :requestType")
  SelfServiceRegistrationJpaEntity findByExternalAuthorizationTokenAndRequestType(
      @Param("token") String externalAuthorizationToken,
      @Param("requestType") SelfServiceRequestType requestType);
}
