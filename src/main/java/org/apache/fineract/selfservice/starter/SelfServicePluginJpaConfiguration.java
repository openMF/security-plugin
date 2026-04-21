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
package org.apache.fineract.selfservice.starter;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Registers all plugin JPA repositories with Fineract's entityManagerFactory.
 *
 * Fineract runs Spring Data in strict mode (JPA + JDBC both present), so repository
 * beans are only created for packages explicitly declared here. A single broad
 * basePackages covering the entire selfservice namespace handles all current and
 * future plugin repositories in one place.
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "org.apache.fineract.selfservice",
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
public class SelfServicePluginJpaConfiguration {
}
