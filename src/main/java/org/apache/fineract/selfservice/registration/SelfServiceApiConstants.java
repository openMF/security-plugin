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
package org.apache.fineract.selfservice.registration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SelfServiceApiConstants {

    private SelfServiceApiConstants() {

    }

    public static final String accountNumberParamName = "accountNumber";
    public static final String passwordParamName = "password";
    public static final String firstNameParamName = "firstName";
    public static final String middleNameParamName = "middleName";
    public static final String mobileNumberParamName = "mobileNumber";
    public static final String lastNameParamName = "lastName";
    public static final String emailParamName = "email";
    public static final String usernameParamName = "username";
    public static final String authenticationTokenParamName = "authenticationToken";
    public static final String authenticationModeParamName = "authenticationMode";
    public static final String emailModeParamName = "email";
    public static final String mobileModeParamName = "mobile";
    public static final String requestIdParamName = "requestId";
    public static final String legalFormIdParamName = "legalFormId";
    public static final String isStaffParamName = "isStaff";
    public static final String officeIdParamName = "officeId";
    public static final String localeParamName = "locale";
    public static final String dateFormatParamName = "dateFormat";
    public static final String activationDateParamName = "activationDate";
    public static final String clientClassificationIdParamName = "clientClassificationId";
    public static final String clientTypeIdParamName = "clientTypeId";
    // Both keys are intentionally supported because copyFirstPresent accepts either request parameter casing.
    public static final String externalIdParamName = "externalId";
    public static final String externalIDParamName = "externalID";
    public static final String submittedOnDateParamName = "submittedOnDate";
    public static final String activeParamName = "active";
    public static final String addressParamName = "address";
    public static final String datatablesParamName = "datatables";
    public static final String familyMembersParamName = "familyMembers";
    public static final String dateOfBirthParamName = "dateOfBirth";
    public static final String genderIdParamName = "genderId";
    // Lowercase keys are preserved for self-enrollment payload compatibility alongside the camelCase variants.
    public static final String firstnameParamName = "firstname";
    public static final String middlenameParamName = "middlename";
    public static final String lastnameParamName = "lastname";
    public static final String createRequestSuccessMessage = "Self service request created.";
    
    public static final Set<String> REGISTRATION_REQUEST_DATA_PARAMETERS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(usernameParamName, accountNumberParamName, passwordParamName, firstNameParamName,
                    mobileNumberParamName, lastNameParamName, emailParamName, authenticationModeParamName,middleNameParamName)));
    
    public static final Set<String> CREATE_USER_REQUEST_DATA_PARAMETERS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(requestIdParamName, authenticationTokenParamName)));
    
    public static final List<Object> SUPPORTED_AUTHENTICATION_MODE_PARAMETERS = List
            .copyOf(Arrays.asList(emailModeParamName, mobileModeParamName));
            
    public static final Set<String> SELF_ENROLLMENT_DATA_PARAMETERS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(usernameParamName, passwordParamName, firstNameParamName,
                    mobileNumberParamName, lastNameParamName, emailParamName, authenticationModeParamName, middleNameParamName,
                    firstnameParamName, middlenameParamName, lastnameParamName, officeIdParamName, localeParamName, dateFormatParamName,
                    legalFormIdParamName, isStaffParamName, clientClassificationIdParamName, clientTypeIdParamName, externalIdParamName,
                    externalIDParamName, submittedOnDateParamName, activationDateParamName, activeParamName, addressParamName,
                    datatablesParamName, familyMembersParamName, dateOfBirthParamName, genderIdParamName)));
    
    public static final String SELF_SERVICE_USER_ROLE = "Self Service User";

}
