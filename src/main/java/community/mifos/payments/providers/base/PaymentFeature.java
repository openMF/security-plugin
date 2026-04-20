/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.base;

/**
 *
 * @author fintecheando
 */
public enum PaymentFeature {
    QR_CODE_PAYMENT,
    RECURRING_PAYMENTS,
    SCHEDULED_PAYMENTS,
    INSTANT_REFUND,
    WEBHOOK_NOTIFICATIONS,
    OFFLINE_CAPABLE
}