/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.selfservice.security.service;

import org.apache.fineract.selfservice.security.domain.PlatformSelfServiceUser;
import org.apache.fineract.selfservice.security.domain.PlatformSelfServiceUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Used in securityContext.xml as implementation of spring security's {@link UserDetailsService}.
 */
@Service("selfServiceUserDetailsService")
public class TenantAwareJpaPlatformSelfServiceUserDetailsService implements PlatformSelfServiceUserDetailsService {

    @Autowired
    private PlatformSelfServiceUserRepository platformUserRepository;

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException, DataAccessException {

        // Retrieve active users only
        final boolean deleted = false;
        final boolean enabled = true;

        final PlatformSelfServiceUser appUser = this.platformUserRepository.findByUsernameAndDeletedAndEnabled(username, deleted, enabled);

        if (appUser == null) {
            throw new UsernameNotFoundException(username + ": not found");
        }

        return appUser;
    }
}
