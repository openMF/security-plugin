/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.selfservice.registration.api;

import io.swagger.v3.oas.annotations.media.Schema;

public class SelfServiceEnrollmentRequest {

    @Schema(example = "vilma")
    public String username;

    @Schema(example = "SecretPassword123#")
    public String password;

    @Schema(example = "Vilma")
    public String firstName;

    @Schema(example = "Flintstone")
    public String lastName;

    @Schema(example = "5522649498")
    public String mobileNumber;

    @Schema(example = "vilma@hotmail.com")
    public String email;

    @Schema(example = "email")
    public String authenticationMode;

    @Schema(example = "1")
    public Long legalFormId;

    @Schema(example = "17 febrero 2026")
    public String submittedOnDate;

    @Schema(example = "false")
    public Boolean isStaff;
    
    @Schema(example = "false")
    public Boolean active;

}
