/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.pix;

import community.mifos.payments.infrastructure.exception.PaymentException;
import java.math.BigDecimal;

/**
 * Exception for PIX (Brazil) specific payment errors.
 * Extends PaymentException with BACEN error codes and PIX-specific context.
 */
public class PixPaymentException extends PaymentException {

    private static final long serialVersionUID = 1L;

    /**
     * BACEN (Banco Central do Brasil) error code from DICT or SPI.
     * Example: "BE1", "C03", "J01"
     */
    private final String bacenErrorCode;

    /**
     * PIX transaction ID (txid) if available.
     */
    private final String pixTransactionId;

    /**
     * Type of PIX operation that failed.
     */
    private final PixOperation operation;

    /**
     * HTTP status code from BACEN API response.
     */
    private final int httpStatusCode;

    /**
     * Whether this error is retryable.
     */
    private final boolean retryable;

    public PixPaymentException(String message) {
        super(message, "PIX_ERROR");
        this.bacenErrorCode = null;
        this.pixTransactionId = null;
        this.operation = null;
        this.httpStatusCode = 0;
        this.retryable = false;
    }

    public PixPaymentException(String message, String bacenErrorCode) {
        super(message, "PIX_ERROR");
        this.bacenErrorCode = bacenErrorCode;
        this.pixTransactionId = null;
        this.operation = null;
        this.httpStatusCode = 0;
        this.retryable = isRetryableBacenCode(bacenErrorCode);
    }

    public PixPaymentException(String message, String bacenErrorCode, String pixTransactionId) {
        super(message, "PIX_ERROR");
        this.bacenErrorCode = bacenErrorCode;
        this.pixTransactionId = pixTransactionId;
        this.operation = null;
        this.httpStatusCode = 0;
        this.retryable = isRetryableBacenCode(bacenErrorCode);
    }

    public PixPaymentException(String message, String bacenErrorCode, 
                               String pixTransactionId, Throwable cause) {
        super(message, "PIX_ERROR", cause);
        this.bacenErrorCode = bacenErrorCode;
        this.pixTransactionId = pixTransactionId;
        this.operation = null;
        this.httpStatusCode = 0;
        this.retryable = isRetryableBacenCode(bacenErrorCode);
    }

    public PixPaymentException(String message, String bacenErrorCode,
                               String pixTransactionId, PixOperation operation,
                               int httpStatusCode, boolean retryable) {
        super(message, "PIX_ERROR");
        this.bacenErrorCode = bacenErrorCode;
        this.pixTransactionId = pixTransactionId;
        this.operation = operation;
        this.httpStatusCode = httpStatusCode;
        this.retryable = retryable;
    }

    public PixPaymentException(String message, String bacenErrorCode,
                               String pixTransactionId, PixOperation operation,
                               int httpStatusCode, boolean retryable, Throwable cause) {
        super(message, "PIX_ERROR", cause);
        this.bacenErrorCode = bacenErrorCode;
        this.pixTransactionId = pixTransactionId;
        this.operation = operation;
        this.httpStatusCode = httpStatusCode;
        this.retryable = retryable;
    }

    // -------------------------------------------------------------------------
    // Factory Methods for Common BACEN Errors
    // -------------------------------------------------------------------------

    /**
     * Invalid PIX key (DICT error).
     * BACEN code: BE1
     */
    public static PixPaymentException invalidPixKey(String pixKey) {
        return new PixPaymentException(
            "Invalid or non-existent PIX key: " + maskPixKey(pixKey),
            "BE1",
            null,
            PixOperation.DICT_LOOKUP,
            404,
            false
        );
    }

    /**
     * PIX key not found in DICT.
     * BACEN code: C03
     */
    public static PixPaymentException pixKeyNotFound(String pixKey) {
        return new PixPaymentException(
            "PIX key not found in DICT: " + maskPixKey(pixKey),
            "C03",
            null,
            PixOperation.DICT_LOOKUP,
            404,
            false
        );
    }

    /**
     * Insufficient funds.
     * BACEN code: J01
     */
    public static PixPaymentException insufficientFunds(String txid, BigDecimal amount) {
        return new PixPaymentException(
            "Insufficient funds for PIX transaction: " + txid + " amount: " + amount,
            "J01",
            txid,
            PixOperation.TRANSFER,
            422,
            false
        );
    }

    /**
     * Transaction timeout.
     * BACEN code: J02
     */
    public static PixPaymentException transactionTimeout(String txid) {
        return new PixPaymentException(
            "PIX transaction timeout: " + txid,
            "J02",
            txid,
            PixOperation.TRANSFER,
            504,
            true
        );
    }

    /**
     * Invalid QR code.
     * BACEN code: J05
     */
    public static PixPaymentException invalidQrCode(String reason) {
        return new PixPaymentException(
            "Invalid PIX QR code: " + reason,
            "J05",
            null,
            PixOperation.QR_GENERATION,
            400,
            false
        );
    }

    /**
     * Duplicate transaction (idempotency).
     * BACEN code: J08
     */
    public static PixPaymentException duplicateTransaction(String txid) {
        return new PixPaymentException(
            "Duplicate PIX transaction: " + txid,
            "J08",
            txid,
            PixOperation.TRANSFER,
            409,
            false
        );
    }

    /**
     * Transaction already completed (cannot cancel).
     * BACEN code: J10
     */
    public static PixPaymentException alreadyCompleted(String txid) {
        return new PixPaymentException(
            "PIX transaction already completed: " + txid,
            "J10",
            txid,
            PixOperation.CANCELLATION,
            422,
            false
        );
    }

    /**
     * DICT service unavailable.
     * BACEN code: C08
     */
    public static PixPaymentException dictUnavailable(Throwable cause) {
        return new PixPaymentException(
            "DICT service temporarily unavailable",
            "C08",
            null,
            PixOperation.DICT_LOOKUP,
            503,
            true,
            cause
        );
    }

    /**
     * SPI (Instant Payment System) unavailable.
     * BACEN code: SPI01
     */
    public static PixPaymentException spiUnavailable(Throwable cause) {
        return new PixPaymentException(
            "SPI (Instant Payment System) temporarily unavailable",
            "SPI01",
            null,
            PixOperation.TRANSFER,
            503,
            true,
            cause
        );
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private static boolean isRetryableBacenCode(String bacenErrorCode) {
        if (bacenErrorCode == null) return false;
        // Retryable codes: timeouts, service unavailable, rate limiting
        return switch (bacenErrorCode) {
            case "J02", "C08", "SPI01", "J03", "J04" -> true;
            default -> false;
        };
    }

    private static String maskPixKey(String pixKey) {
        if (pixKey == null || pixKey.length() < 4) return "***";
        return pixKey.substring(0, 2) + "***" + pixKey.substring(pixKey.length() - 2);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getBacenErrorCode() {
        return bacenErrorCode;
    }

    public String getPixTransactionId() {
        return pixTransactionId;
    }

    public PixOperation getOperation() {
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
        return "PixPaymentException{" +
                "message='" + getMessage() + '\'' +
                ", bacenErrorCode='" + bacenErrorCode + '\'' +
                ", pixTransactionId='" + pixTransactionId + '\'' +
                ", operation=" + operation +
                ", httpStatusCode=" + httpStatusCode +
                ", retryable=" + retryable +
                ", providerCode='" + getProviderCode() + '\'' +
                '}';
    }

    // -------------------------------------------------------------------------
    // Inner Enum
    // -------------------------------------------------------------------------

    public enum PixOperation {
        DICT_LOOKUP,      // Key validation in DICT
        TRANSFER,         // Payment transfer
        QR_GENERATION,    // QR code creation
        CANCELLATION,     // Transaction cancellation
        REFUND,           // Devolução (refund)
        WEBHOOK,          // Webhook processing
        RECONCILIATION    // End-of-day reconciliation
    }
}