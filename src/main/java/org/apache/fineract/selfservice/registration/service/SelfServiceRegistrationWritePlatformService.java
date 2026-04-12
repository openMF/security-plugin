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

import org.apache.fineract.selfservice.registration.domain.SelfServiceRegistration;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUser;

public interface SelfServiceRegistrationWritePlatformService {

    SelfServiceRegistration createRegistrationRequest(String apiRequestBodyAsJson);

    AppSelfServiceUser createSelfServiceUser(String apiRequestBodyAsJson);

    /**
     * Executes the legacy token-confirmation flow when {@code requestId} and
     * {@code authenticationToken} are supplied; otherwise performs one-shot self-enrollment using
     * client creation fields.
     *
     * @param apiRequestBodyAsJson JSON request body containing either legacy confirmation fields
     *     ({@code requestId}, {@code authenticationToken}) or self-enrollment fields accepted by
     *     the enrollment endpoint
     * @return the created self-service user, including the generated identifier after persistence
     * @throws org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException
     *     if required fields are missing or invalid
     * @throws org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException if a
     *     duplicate user or other integrity issue is detected
     * @throws org.apache.fineract.selfservice.registration.exception.SelfServiceEnrollmentConflictException
     *     if enrollment fails with a conflict that maps to HTTP 409
     *
     * Side effects: persists registration or enrollment data, may create a client and a linked
     * self-service user, and may trigger downstream notification or mapping writes.
     */
    AppSelfServiceUser createSelfServiceUserOrEnroll(String apiRequestBodyAsJson);

    /**
     * Executes atomic one-shot self-enrollment by provisioning a Client and User.
     * 
     * @param apiRequestBodyAsJson The incoming request containing enrollment data matching {@code SelfServiceEnrollmentRequest}.
     * @return The freshly created AppSelfServiceUser object.
     */
    AppSelfServiceUser selfEnroll(String apiRequestBodyAsJson);
}
