/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.infrastructure.audit;

public enum AuditAction {
    PAYMENT_INITIATED,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    PAYMENT_CANCELLED,
    PAYMENT_REFUNDED,
    STATUS_CHANGED,
    PROVIDER_API_CALL,
    WEBHOOK_RECEIVED,
    WEBHOOK_PROCESSED,
    VALIDATION_FAILED,
    SIGNATURE_INVALID,
    RATE_LIMIT_HIT,
    RECONCILIATION_MISMATCH,
    SECURITY_EVENT
}

