/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.spei;

import community.mifos.payments.infrastructure.exception.PaymentException;

import java.math.BigDecimal;

/**
 * Exception for SPEI (Mexico) specific payment errors.
 * Extends PaymentException with Banxico error codes and SPEI-specific context.
 */
public class SpeiPaymentException extends PaymentException {

    private static final long serialVersionUID = 1L;

    /**
     * Banxico error code from SPEI response.
     * Example: "E001", "E002", "C001"
     */
    private final String banxicoErrorCode;

    /**
     * SPEI transaction ID (idOrden) if available.
     */
    private final String speiTransactionId;

    /**
     * Type of SPEI operation that failed.
     */
    private final SpeiOperation operation;

    /**
     * HTTP status code from Banxico API response.
     */
    private final int httpStatusCode;

    /**
     * Whether this error is retryable.
     */
    private final boolean retryable;

    public SpeiPaymentException(String message) {
        super(message, "SPEI_ERROR");
        this.banxicoErrorCode = null;
        this.speiTransactionId = null;
        this.operation = null;
        this.httpStatusCode = 0;
        this.retryable = false;
    }

    public SpeiPaymentException(String message, String banxicoErrorCode) {
        super(message, "SPEI_ERROR");
        this.banxicoErrorCode = banxicoErrorCode;
        this.speiTransactionId = null;
        this.operation = null;
        this.httpStatusCode = 0;
        this.retryable = isRetryableBanxicoCode(banxicoErrorCode);
    }

    public SpeiPaymentException(String message, String banxicoErrorCode, String speiTransactionId) {
        super(message, "SPEI_ERROR");
        this.banxicoErrorCode = banxicoErrorCode;
        this.speiTransactionId = speiTransactionId;
        this.operation = null;
        this.httpStatusCode = 0;
        this.retryable = isRetryableBanxicoCode(banxicoErrorCode);
    }

    public SpeiPaymentException(String message, String banxicoErrorCode,
                                String speiTransactionId, Throwable cause) {
        super(message, "SPEI_ERROR", cause);
        this.banxicoErrorCode = banxicoErrorCode;
        this.speiTransactionId = speiTransactionId;
        this.operation = null;
        this.httpStatusCode = 0;
        this.retryable = isRetryableBanxicoCode(banxicoErrorCode);
    }

    public SpeiPaymentException(String message, String banxicoErrorCode,
                                String speiTransactionId, SpeiOperation operation,
                                int httpStatusCode, boolean retryable) {
        super(message, "SPEI_ERROR");
        this.banxicoErrorCode = banxicoErrorCode;
        this.speiTransactionId = speiTransactionId;
        this.operation = operation;
        this.httpStatusCode = httpStatusCode;
        this.retryable = retryable;
    }

    public SpeiPaymentException(String message, String banxicoErrorCode,
                                String speiTransactionId, SpeiOperation operation,
                                int httpStatusCode, boolean retryable, Throwable cause) {
        super(message, "SPEI_ERROR", cause);
        this.banxicoErrorCode = banxicoErrorCode;
        this.speiTransactionId = speiTransactionId;
        this.operation = operation;
        this.httpStatusCode = httpStatusCode;
        this.retryable = retryable;
    }

    // -------------------------------------------------------------------------
    // Factory Methods for Common Banxico/SPEI Errors
    // -------------------------------------------------------------------------

    /**
     * Invalid CLABE format or checksum.
     * Banxico code: E001
     */
    public static SpeiPaymentException invalidClabe(String clabe) {
        return new SpeiPaymentException(
            "Invalid CLABE format or checksum: " + maskClabe(clabe),
            "E001",
            null,
            SpeiOperation.VALIDATION,
            400,
            false
        );
    }

    /**
     * CLABE not found or not active.
     * Banxico code: E002
     */
    public static SpeiPaymentException clabeNotFound(String clabe) {
        return new SpeiPaymentException(
            "CLABE not found or inactive in SPEI: " + maskClabe(clabe),
            "E002",
            null,
            SpeiOperation.VALIDATION,
            404,
            false
        );
    }

    /**
     * Amount exceeds SPEI same-day limit (MXN 800,000).
     * Banxico code: E003
     */
    public static SpeiPaymentException amountExceedsLimit(BigDecimal amount, BigDecimal limit) {
        return new SpeiPaymentException(
            "Amount " + amount + " exceeds SPEI same-day limit of " + limit,
            "E003",
            null,
            SpeiOperation.TRANSFER,
            422,
            false
        );
    }

    /**
     * Amount exceeds CoDi limit (MXN 30,000).
     * Banxico code: E004
     */
    public static SpeiPaymentException codiAmountExceedsLimit(BigDecimal amount, BigDecimal limit) {
        return new SpeiPaymentException(
            "Amount " + amount + " exceeds CoDi limit of " + limit,
            "E004",
            null,
            SpeiOperation.CODI_TRANSFER,
            422,
            false
        );
    }

    /**
     * Insufficient funds.
     * Banxico code: E005
     */
    public static SpeiPaymentException insufficientFunds(String idOrden) {
        return new SpeiPaymentException(
            "Insufficient funds for SPEI transaction: " + idOrden,
            "E005",
            idOrden,
            SpeiOperation.TRANSFER,
            422,
            false
        );
    }

    /**
     * Transaction timeout at Banxico.
     * Banxico code: T001
     */
    public static SpeiPaymentException transactionTimeout(String idOrden) {
        return new SpeiPaymentException(
            "SPEI transaction timeout: " + idOrden,
            "T001",
            idOrden,
            SpeiOperation.TRANSFER,
            504,
            true
        );
    }

    /**
     * Duplicate transaction detected.
     * Banxico code: E006
     */
    public static SpeiPaymentException duplicateTransaction(String idOrden) {
        return new SpeiPaymentException(
            "Duplicate SPEI transaction detected: " + idOrden,
            "E006",
            idOrden,
            SpeiOperation.TRANSFER,
            409,
            false
        );
    }

    /**
     * Transaction already completed (cannot cancel).
     * Banxico code: E007
     */
    public static SpeiPaymentException alreadyCompleted(String idOrden) {
        return new SpeiPaymentException(
            "SPEI transaction already completed and cannot be cancelled: " + idOrden,
            "E007",
            idOrden,
            SpeiOperation.CANCELLATION,
            422,
            false
        );
    }

    /**
     * Outside SPEI operating hours (06:00-17:30 CST).
     * Banxico code: E008
     */
    public static SpeiPaymentException outsideOperatingHours() {
        return new SpeiPaymentException(
            "Transaction requested outside SPEI operating hours (06:00-17:30 CST)",
            "E008",
            null,
            SpeiOperation.TRANSFER,
            422,
            false
        );
    }

    /**
     * CoDi not enabled for this account.
     * Banxico code: E009
     */
    public static SpeiPaymentException codiNotEnabled() {
        return new SpeiPaymentException(
            "CoDi (Cobro Digital) is not enabled for this account",
            "E009",
            null,
            SpeiOperation.CODI_TRANSFER,
            422,
            false
        );
    }

    /**
     * Invalid CoDi QR code.
     * Banxico code: E010
     */
    public static SpeiPaymentException invalidCodiQr(String reason) {
        return new SpeiPaymentException(
            "Invalid CoDi QR code: " + reason,
            "E010",
            null,
            SpeiOperation.CODI_GENERATION,
            400,
            false
        );
    }

    /**
     * Banxico/SPEI service unavailable.
     * Banxico code: S001
     */
    public static SpeiPaymentException serviceUnavailable(Throwable cause) {
        return new SpeiPaymentException(
            "SPEI service temporarily unavailable",
            "S001",
            null,
            SpeiOperation.TRANSFER,
            503,
            true,
            cause
        );
    }

    /**
     * Invalid certificate or mutual TLS failure.
     * Banxico code: S002
     */
    public static SpeiPaymentException certificateError(String details) {
        return new SpeiPaymentException(
            "SPEI certificate authentication failed: " + details,
            "S002",
            null,
            SpeiOperation.AUTHENTICATION,
            401,
            false
        );
    }

    /**
     * Webhook signature validation failed.
     * Banxico code: W001
     */
    public static SpeiPaymentException invalidWebhookSignature() {
        return new SpeiPaymentException(
            "SPEI webhook signature validation failed",
            "W001",
            null,
            SpeiOperation.WEBHOOK,
            401,
            false
        );
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private static boolean isRetryableBanxicoCode(String banxicoErrorCode) {
        if (banxicoErrorCode == null) return false;
        return switch (banxicoErrorCode) {
            case "T001", "S001", "S003", "G001" -> true;
            default -> false;
        };
    }

    private static String maskClabe(String clabe) {
        if (clabe == null || clabe.length() < 6) return "***";
        return clabe.substring(0, 3) + "**************" + clabe.substring(clabe.length() - 3);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getBanxicoErrorCode() {
        return banxicoErrorCode;
    }

    public String getSpeiTransactionId() {
        return speiTransactionId;
    }

    public SpeiOperation getOperation() {
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
        return "SpeiPaymentException{" +
                "message='" + getMessage() + '\'' +
                ", banxicoErrorCode='" + banxicoErrorCode + '\'' +
                ", speiTransactionId='" + speiTransactionId + '\'' +
                ", operation=" + operation +
                ", httpStatusCode=" + httpStatusCode +
                ", retryable=" + retryable +
                ", providerCode='" + getProviderCode() + '\'' +
                '}';
    }

    // -------------------------------------------------------------------------
    // Inner Enum
    // -------------------------------------------------------------------------

    public enum SpeiOperation {
        VALIDATION,      // CLABE validation
        TRANSFER,        // SPEI transfer
        CODI_TRANSFER,   // CoDi payment
        CODI_GENERATION, // CoDi QR generation
        CANCELLATION,    // Transaction cancellation
        REVERSAL,        // Return/refund (devolución)
        WEBHOOK,         // Webhook processing
        AUTHENTICATION,  // Mutual TLS / OAuth
        RECONCILIATION   // End-of-day reconciliation
    }
}