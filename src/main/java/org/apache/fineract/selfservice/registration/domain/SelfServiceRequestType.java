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
package org.apache.fineract.selfservice.registration.domain;

/**
 * Discriminator for self-service registration records stored in {@code request_audit_table}.
 *
 * <p>No JPA or framework annotations are permitted in this enum; the persistence mapping
 * (EnumType.STRING) is configured in the infrastructure adapter entity.
 */
public enum SelfServiceRequestType {

    /** Standard self-service registration for an existing client. */
    REGISTRATION,

    /** Combined client creation and self-service user enrolment. */
    CLIENT_USER_ENROLLMENT,

    /** Forgot-password / password-reset flow. */
    FORGOT_PASSWORD
}
