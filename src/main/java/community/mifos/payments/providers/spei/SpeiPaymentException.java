/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.spei;

import community.mifos.payments.infrastructure.exception.PaymentException;


/**
 * Exception for SPEI (Mexico) specific payment errors.
 * Extends PaymentException with Banxico (Banco de México) error codes.
 */
public class SpeiPaymentException extends PaymentException {

    private static final long serialVersionUID = 1L;

    /**
     * Banxico/SPEI error code.
     * Examples: "SP001", "SP002", etc.
     */
    private final String speiErrorCode;

    /**
     * SPEI order ID (idOrden) if available.
     */
    private final String speiOrderId;

    /**
     * Type of SPEI operation that failed.
     */
    private final SpeiOperation operation;

    /**
     * HTTP status code from Banxico/SPEI API response.
     */
    private final int httpStatusCode;

    /**
     * Whether this error is retryable.
     */
    private final boolean retryable;

    public SpeiPaymentException(String message) {
        super(message, "SPEI_ERROR");
        this.banxicoErrorCode = null;
        this.speiOrderId = null;
        this.operation = null;
        this.httpStatusCode = 0;
        this.retryable = false;
    }

    public SpeiPaymentException(String message, String banxicoErrorCode) {
        super(message, "SPEI_ERROR");
        this.banxicoErrorCode = banxicoErrorCode;
        this.speiOrderId = null;
        this.operation = null;
        this.httpStatusCode = 0;
        this.retryable = isRetryableBanxicoCode(banxicoErrorCode);
    }
    
}

    