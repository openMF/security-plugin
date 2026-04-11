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
package org.apache.fineract.selfservice.useradministration.domain;

// TODO(MX230-domain-jpa-ban): Migrate AppSelfServiceUserClientMapping to DDD split.
// This class is a JPA entity living in the domain package, which violates the
// domain-layer framework-independence rule enforced in .coderabbit.yml.
//
// Required actions (tracked in issue / PR MX230):
//   1. Create a pure domain value-object / record that holds only appUserId + clientId.
//   2. Move all JPA annotations to a new
//      useradministration.infrastructure.persistence.AppSelfServiceUserClientMappingJpaEntity.
//   3. Replace AppSelfServiceUserClientMappingRepository (currently JpaRepository<this, Long>)
//      with a domain interface, backed by an infrastructure adapter.
//
// Blocked by: AppSelfServiceUser still being a JPA entity (see its own TODO).
// Until both migrations land, CodeRabbit will flag JPA imports here.

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.client.domain.Client;

@Entity
@Table(name = "m_selfservice_user_client_mapping")
public class AppSelfServiceUserClientMapping extends AbstractPersistableCustom<Long> {

    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "appuser_id", nullable = false, foreignKey = @ForeignKey(name = "users_appusers"))
    private AppSelfServiceUser appUser;

    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "client_id", nullable = false, foreignKey = @ForeignKey(name = "clients_appusers"))
    private Client client;

    public AppSelfServiceUserClientMapping() {}

    public AppSelfServiceUserClientMapping(AppSelfServiceUser appUser, Client client) {
      this.appUser = appUser;
      this.client = client;
    }

    public Client getClient() {
      return this.client;
    }

    public AppSelfServiceUser getAppUser() {
      return appUser;
    }

    @Override
    public boolean equals(Object obj) {

      if (null == obj) {
        return false;
      }

      if (this == obj) {
        return true;
      }

      if (!(obj instanceof AppSelfServiceUserClientMapping)) {
        return false;
      }

      AppSelfServiceUserClientMapping that = (AppSelfServiceUserClientMapping) obj;
      return null == this.client.getId() ? false : this.client.getId().equals(that.client.getId());
    }

    @Override
    public int hashCode() {

      int hashCode = 17;
      hashCode += null == this.client ? 0 : this.client.getId().hashCode() * 31;
      return hashCode;
    }
}
