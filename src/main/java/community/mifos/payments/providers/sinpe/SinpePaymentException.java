/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.sinpe;

import community.mifos.payments.infrastructure.exception.PaymentException;

import java.math.BigDecimal;

/**
 * Exception for SINPE (Costa Rica) specific payment errors.
 * Extends PaymentException with BCCR (Banco Central de Costa Rica) error codes.
 */
public class SinpePaymentException extends PaymentException {

    private static final long serialVersionUID = 1L;

    /**
     * BCCR error code from SINPE response.
     */
    private final String bccrErrorCode;

    /**
     * SINPE transaction reference (número de comprobante) if available.
     */
    private final String sinpeReference;

    /**
     * Type of SINPE operation that failed.
     */
    private final SinpeOperation operation;

    /**
     * HTTP status code from BCCR/SINPE API response.
     */
    private final int httpStatusCode;

    /**
     * Whether this error is retryable.
     */
    private final boolean retryable;

    public SinpePaymentException(String message) {
        super(message, "SINPE_ERROR");
        this.bccrErrorCode = null;
        this.sinpeReference = null;
        this.operation = null;
        this.httpStatusCode = 0;
        this.retryable = false;
    }

    public SinpePaymentException(String message, String bccrErrorCode) {
        super(message, "SINPE_ERROR");
        this.bccrErrorCode = bccrErrorCode;
        this.sinpeReference = null;
        this.operation = null;
        this.httpStatusCode = 0;
        this.retryable = isRetryableBccrCode(bccrErrorCode);
    }

    public SinpePaymentException(String message, String bccrErrorCode, String sinpeReference) {
        super(message, "SINPE_ERROR");
        this.bccrErrorCode = bccrErrorCode;
        this.sinpeReference = sinpeReference;
        this.operation = null;
        this.httpStatusCode = 0;
        this.retryable = isRetryableBccrCode(bccrErrorCode);
    }

    public SinpePaymentException(String message, String bccrErrorCode,
                                 String sinpeReference, Throwable cause) {
        super(message, "SINPE_ERROR", cause);
        this.bccrErrorCode = bccrErrorCode;
        this.sinpeReference = sinpeReference;
        this.operation = null;
        this.httpStatusCode = 0;
        this.retryable = isRetryableBccrCode(bccrErrorCode);
    }

    public SinpePaymentException(String message, String bccrErrorCode,
                                 String sinpeReference, SinpeOperation operation,
                                 int httpStatusCode, boolean retryable) {
        super(message, "SINPE_ERROR");
        this.bccrErrorCode = bccrErrorCode;
        this.sinpeReference = sinpeReference;
        this.operation = operation;
        this.httpStatusCode = httpStatusCode;
        this.retryable = retryable;
    }

    public SinpePaymentException(String message, String bccrErrorCode,
                                 String sinpeReference, SinpeOperation operation,
                                 int httpStatusCode, boolean retryable, Throwable cause) {
        super(message, "SINPE_ERROR", cause);
        this.bccrErrorCode = bccrErrorCode;
        this.sinpeReference = sinpeReference;
        this.operation = operation;
        this.httpStatusCode = httpStatusCode;
        this.retryable = retryable;
    }

    // -------------------------------------------------------------------------
    // Factory Methods for Common BCCR/SINPE Errors
    // -------------------------------------------------------------------------

    /**
     * Phone number not registered in SINPE Móvil.
     * BCCR code: SM001
     */
    public static SinpePaymentException phoneNotRegistered(String phoneNumber) {
        return new SinpePaymentException(
            "Phone number not registered in SINPE Móvil: " + maskPhone(phoneNumber),
            "SM001",
            null,
            SinpeOperation.VALIDATION,
            404,
            false
        );
    }

    /**
     * Invalid IBAN format.
     * BCCR code: IB001
     */
    public static SinpePaymentException invalidIban(String iban) {
        return new SinpePaymentException(
            "Invalid Costa Rican IBAN format: " + iban,
            "IB001",
            null,
            SinpeOperation.VALIDATION,
            400,
            false
        );
    }

    /**
     * Amount exceeds SINPE Móvil limit (CRC 100,000).
     * BCCR code: SM002
     */
    public static SinpePaymentException amountExceedsMobileLimit(BigDecimal amount, BigDecimal limit) {
        return new SinpePaymentException(
            "Amount " + amount + " exceeds SINPE Móvil limit of " + limit,
            "SM002",
            null,
            SinpeOperation.TRANSFER,
            422,
            false
        );
    }

    /**
     * Insufficient funds.
     * BCCR code: TF001
     */
    public static SinpePaymentException insufficientFunds(String reference) {
        return new SinpePaymentException(
            "Insufficient funds for SINPE transaction: " + reference,
            "TF001",
            reference,
            SinpeOperation.TRANSFER,
            422,
            false
        );
    }

    /**
     * Transaction timeout at BCCR.
     * BCCR code: SYS001
     */
    public static SinpePaymentException transactionTimeout(String reference) {
        return new SinpePaymentException(
            "SINPE transaction timeout: " + reference,
            "SYS001",
            reference,
            SinpeOperation.TRANSFER,
            504,
            true
        );
    }

    /**
     * Duplicate transaction detected.
     * BCCR code: TF002
     */
    public static SinpePaymentException duplicateTransaction(String reference) {
        return new SinpePaymentException(
            "Duplicate SINPE transaction detected: " + reference,
            "TF002",
            reference,
            SinpeOperation.TRANSFER,
            409,
            false
        );
    }

    /**
     * Transaction already completed (cannot reverse).
     * BCCR code: RV001
     */
    public static SinpePaymentException alreadyCompleted(String reference) {
        return new SinpePaymentException(
            "SINPE transaction already completed and cannot be reversed: " + reference,
            "RV001",
            reference,
            SinpeOperation.REVERSAL,
            422,
            false
        );
    }

    /**
     * BCCR/SINPE service unavailable.
     * BCCR code: SYS002
     */
    public static SinpePaymentException serviceUnavailable(Throwable cause) {
        return new SinpePaymentException(
            "SINPE service temporarily unavailable",
            "SYS002",
            null,
            SinpeOperation.TRANSFER,
            503,
            true,
            cause
        );
    }

    /**
     * Invalid transaction state for requested operation.
     * BCCR code: TF003
     */
    public static SinpePaymentException invalidTransactionState(String reference, String state) {
        return new SinpePaymentException(
            "Invalid SINPE transaction state '" + state + "' for operation on: " + reference,
            "TF003",
            reference,
            SinpeOperation.TRANSFER,
            409,
            false
        );
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private static boolean isRetryableBccrCode(String bccrErrorCode) {
        if (bccrErrorCode == null) return false;
        return switch (bccrErrorCode) {
            case "SYS001", "SYS002", "SYS003", "GW001" -> true;
            default -> false;
        };
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return phone.substring(0, 2) + "****" + phone.substring(phone.length() - 2);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getBccrErrorCode() {
        return bccrErrorCode;
    }

    public String getSinpeReference() {
        return sinpeReference;
    }

    public SinpeOperation getOperation() {
        return operation;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    @Override
    public String toString() {
        return "SinpePaymentException{" +
                "message='" + getMessage() + '\'' +
                ", bccrErrorCode='" + bccrErrorCode + '\'' +
                ", sinpeReference='" + sinpeReference + '\'' +
                ", operation=" + operation +
                ", httpStatusCode=" + httpStatusCode +
                ", retryable=" + retryable +
                ", providerCode='" + getProviderCode() + '\'' +
                '}';
    }

    // -------------------------------------------------------------------------
    // Inner Enum
    // -------------------------------------------------------------------------

    public enum SinpeOperation {
        VALIDATION,    // Phone/IBAN validation
        TRANSFER,      // SINPE Móvil or SINPE IBAN transfer
        REVERSAL,      // Transaction reversal/refund
        NOTIFICATION,  // SMS/push notification
        RECONCILIATION // End-of-day reconciliation
    }
}