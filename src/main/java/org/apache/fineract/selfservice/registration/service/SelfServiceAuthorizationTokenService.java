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
package org.apache.fineract.selfservice.registration.service;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.core.env.Environment;

public class SelfServiceAuthorizationTokenService {

    private final long expiryMinutes;

    public SelfServiceAuthorizationTokenService(Environment env) {
        this.expiryMinutes = Long.parseLong(
                env.getProperty("fineract.selfservice.registration.token.expiry-minutes", "60"));
    }

    public String generateToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    public LocalDateTime calculateExpiry(LocalDateTime createdAt) {
        return createdAt.plusMinutes(expiryMinutes);
    }
}
