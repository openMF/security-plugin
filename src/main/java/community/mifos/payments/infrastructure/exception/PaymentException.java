/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.infrastructure.exception;

/**
 * Base exception for all payment-related errors.
 * Extends RuntimeException for transactional rollback behavior.
 */
public abstract class PaymentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Error code for categorization (INVALID_REQUEST, PROVIDER_ERROR, etc.)
     */
    private final String errorCode;

    /**
     * Provider that threw the exception (PIX, SPEI, SINPE, INTERNAL)
     */
    private String providerCode;

    /**
     * Transaction reference code if available
     */
    private String referenceCode;

    protected PaymentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    protected PaymentException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }
}