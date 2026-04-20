/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * REST API payload for POST /selfservice/payments/transfer
 */
public record FastPaymentRequest(
    @NotNull @Positive BigDecimal amount,
    @NotBlank @Size(min = 3, max = 3) String currency,
    @NotBlank @Size(max = 100) String recipientIdentifier,
    @Size(max = 200) String recipientName,
    @Size(max = 500) String description,
    @NotNull Long sourceAccountId,
    Long sourceClientId,
    @NotBlank @Size(min = 2, max = 2) String countryCode,
    @Size(max = 10) String providerCode,
    @Size(max = 20) String channel,
    @Size(max = 20) String paymentMethod,
    @Size(max = 100) String idempotencyKey,
    @Size(max = 500) String callbackUrl
) {}