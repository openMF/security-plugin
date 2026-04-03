/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.useradministration.domain;


import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.springframework.stereotype.Component;

@Component
public class SelfServiceUserDomainServiceImpl implements SelfServiceUserDomainService {

    @Override
    public void create(AppSelfServiceUser appUser, Boolean sendPasswordToEmail) {
        throw new PlatformApiDataValidationException(
            "error.msg.self.service.user.creation.not.supported.in.plugin",
            "Self service user registration is not fully supported in the current plugin. " +
            "Use main Fineract or disable registration.",
            null);
    }
}