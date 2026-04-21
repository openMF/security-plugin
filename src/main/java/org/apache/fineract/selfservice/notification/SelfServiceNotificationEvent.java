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
package org.apache.fineract.selfservice.notification;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.springframework.context.ApplicationEvent;

public class SelfServiceNotificationEvent extends ApplicationEvent {

    public enum Type {
        USER_ACTIVATED,
        REGISTRATION_REQUESTED,
        PASSWORD_RESET_REQUESTED
    }

    private final Type type;
    private final Long userId;
    private final String firstname;
    private final String lastname;
    private final String username;
    private final String email;
    private final String mobileNumber;
    private final boolean emailMode;
    private final Object extra;
    private final Locale locale;
    private final FineractPlatformTenant tenant;
    private final HashMap<BusinessDateType, LocalDate> businessDates;

    public SelfServiceNotificationEvent(
            Object source,
            Type type,
            Long userId,
            String firstname,
            String lastname,
            String username,
            String email,
            String mobileNumber,
            boolean emailMode,
            Object extra,
            Locale locale,
            FineractPlatformTenant tenant,
            HashMap<BusinessDateType, LocalDate> businessDates) {
        super(source);
        this.type = type;
        this.userId = userId;
        this.firstname = firstname;
        this.lastname = lastname;
        this.username = username;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.emailMode = emailMode;
        this.extra = extra;
        this.locale = locale;
        this.tenant = tenant;
        this.businessDates = businessDates;
    }

    public Type getType() { return type; }
    public Long getUserId() { return userId; }
    public String getFirstname() { return firstname; }
    public String getLastname() { return lastname; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getMobileNumber() { return mobileNumber; }
    public boolean isEmailMode() { return emailMode; }
    public Object getExtra() { return extra; }
    public Locale getLocale() { return locale; }
    public FineractPlatformTenant getTenant() { return tenant; }
    public HashMap<BusinessDateType, LocalDate> getBusinessDates() { return businessDates; }
}
