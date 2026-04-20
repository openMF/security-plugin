/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.infrastructure.exception;

/**
 * Exception thrown when a duplicate idempotency key is detected,
 * indicating the transaction has already been submitted.
 */
public class DuplicateTransactionException extends PaymentException {

    private static final long serialVersionUID = 1L;

    /**
     * The idempotency key that caused the conflict.
     */
    private final String idempotencyKey;

    /**
     * The existing transaction reference (if known).
     */
    private String existingReferenceCode;

    public DuplicateTransactionException(String idempotencyKey) {
        super("Duplicate transaction detected for idempotency key: " + idempotencyKey,
              "DUPLICATE_TRANSACTION");
        this.idempotencyKey = idempotencyKey;
        this.setReferenceCode(idempotencyKey);
    }

    public DuplicateTransactionException(String idempotencyKey, String existingReferenceCode) {
        super("Duplicate transaction detected for idempotency key: " + idempotencyKey +
              ". Existing reference: " + existingReferenceCode,
              "DUPLICATE_TRANSACTION");
        this.idempotencyKey = idempotencyKey;
        this.existingReferenceCode = existingReferenceCode;
        this.setReferenceCode(idempotencyKey);
    }

    public DuplicateTransactionException(String idempotencyKey, Throwable cause) {
        super("Duplicate transaction detected for idempotency key: " + idempotencyKey,
              "DUPLICATE_TRANSACTION", cause);
        this.idempotencyKey = idempotencyKey;
        this.setReferenceCode(idempotencyKey);
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getExistingReferenceCode() {
        return existingReferenceCode;
    }

    public void setExistingReferenceCode(String existingReferenceCode) {
        this.existingReferenceCode = existingReferenceCode;
    }
}