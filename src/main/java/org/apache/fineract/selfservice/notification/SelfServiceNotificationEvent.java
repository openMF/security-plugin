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
package org.apache.fineract.selfservice.notification;

import java.util.Locale;
import java.util.Objects;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SelfServiceNotificationEvent extends ApplicationEvent {

    public enum Type {

        USER_ACTIVATED("user-activated"), LOGIN_SUCCESS("login-success"), LOGIN_FAILURE("login-failure");

        private final String templatePrefix;

        Type(String templatePrefix) {
            this.templatePrefix = templatePrefix;
        }

        public String getTemplatePrefix() {
            return templatePrefix;
        }
    }

    private final Type type;
    private final Long userId;
    private final String firstName;
    private final String lastName;
    private final String username;
    private final String email;
    private final String mobileNumber;
    private final boolean emailMode;
    private final String ipAddress;
    private final Locale locale;

    /**
     * Creates a new self-service notification event.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     * @param type the notification event type (never {@code null})
     * @param userId the user identifier (never {@code null})
     * @param firstName user's first name (may be {@code null})
     * @param lastName user's last name (may be {@code null})
     * @param username user's login username (may be {@code null})
     * @param email user's email address (may be {@code null})
     * @param mobileNumber user's mobile number (may be {@code null})
     * @param emailMode {@code true} for email delivery, {@code false} for SMS
     * @param ipAddress the originating IP address (may be {@code null})
     * @param locale the locale for notification content (may be {@code null})
     */
    public SelfServiceNotificationEvent(Object source, Type type, Long userId, String firstName, String lastName, String username,
            String email, String mobileNumber, boolean emailMode, String ipAddress, Locale locale) {
        super(source);
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.emailMode = emailMode;
        this.ipAddress = ipAddress;
        this.locale = locale;
    }

    /**
     * Returns a safe, non-PII string representation suitable for logging.
     * Sensitive fields (email, username, mobileNumber, ipAddress) are excluded.
     */
    @Override
    public String toString() {
        return "SelfServiceNotificationEvent[type=" + type + ", userId=" + userId
                + ", emailMode=" + emailMode + ", locale=" + locale + "]";
    }
}
