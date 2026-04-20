/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.infrastructure.exception;

/**
 * Exception thrown when a requested payment transaction is not found
 * in the local database.
 */
public class TransactionNotFoundException extends PaymentException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates exception for missing transaction.
     *
     * @param transactionId the provider or internal transaction ID that was not found
     */
    public TransactionNotFoundException(String transactionId) {
        super("Transaction not found: " + transactionId, "NOT_FOUND");
        this.setReferenceCode(transactionId);
    }

    /**
     * Creates exception with additional context.
     *
     * @param transactionId the transaction ID that was not found
     * @param providerCode  the provider where the lookup was attempted
     */
    public TransactionNotFoundException(String transactionId, String providerCode) {
        super("Transaction not found in " + providerCode + ": " + transactionId, "NOT_FOUND");
        this.setReferenceCode(transactionId);
        this.setProviderCode(providerCode);
    }

    /**
     * Creates exception with cause (e.g., database query failure).
     *
     * @param transactionId the transaction ID that was not found
     * @param cause         the underlying exception
     */
    public TransactionNotFoundException(String transactionId, Throwable cause) {
        super("Transaction not found: " + transactionId, "NOT_FOUND", cause);
        this.setReferenceCode(transactionId);
    }
}