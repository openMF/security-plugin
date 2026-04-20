package community.mifos.payments.providers.spei;

import community.mifos.payments.infrastructure.exception.PaymentException;

import java.math.BigDecimal;

/**
 * Exception for SPEI (Mexico) specific payment errors.
 * Extends PaymentException with Banxico (Banco de México) error codes.
 */
public class SpeiPaymentException extends PaymentException {

    private static final long serialVersionUID = 1L;

    /**
     * Banxico error code from SPEI response.
     * Examples: "R01", "R03", "R14", "R17", "R31"
     */
    private final String banxicoErrorCode;

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

    public SpeiPaymentException(String message, String banxicoErrorCode, String speiOrderId) {
        super(message, "SPEI_ERROR");
        this.banxicoErrorCode = banxicoErrorCode;
        this.speiOrderId = speiOrderId;
        this.operation = null;
        this.httpStatusCode = 0;
        this.retryable = isRetryableBanxicoCode(banxicoErrorCode);
    }

    public SpeiPaymentException(String message, String banxicoErrorCode,
                                String speiOrderId, Throwable cause) {
        super(message, "SPEI_ERROR", cause);
        this.banxicoErrorCode = banxicoErrorCode;
        this.speiOrderId = speiOrderId;
        this.operation = null;
        this.httpStatusCode = 0;
        this.retryable = isRetryableBanxicoCode(banxicoErrorCode);
    }

    public SpeiPaymentException(String message, String banxicoErrorCode,
                                String speiOrderId, SpeiOperation operation,
                                int httpStatusCode, boolean retryable) {
        super(message, "SPEI_ERROR");
        this.banxicoErrorCode = banxicoErrorCode;
        this.speiOrderId = speiOrderId;
        this.operation = operation;
        this.httpStatusCode = httpStatusCode;
        this.retryable = retryable;
    }

    public SpeiPaymentException(String message, String banxicoErrorCode,
                                String speiOrderId, SpeiOperation operation,
                                int httpStatusCode, boolean retryable, Throwable cause) {
        super(message, "SPEI_ERROR", cause);
        this.banxicoErrorCode = banxicoErrorCode;
        this.speiOrderId = speiOrderId;
        this.operation = operation;
        this.httpStatusCode = httpStatusCode;
        this.retryable = retryable;
    }

    // -------------------------------------------------------------------------
    // Factory Methods for Common Banxico/SPEI Errors
    // -------------------------------------------------------------------------

    /**
     * Invalid CLABE format (not 18 digits or invalid checksum).
     * Banxico code: R01
     */
    public static SpeiPaymentException invalidClabe(String clabe) {
        return new SpeiPaymentException(
            "Invalid CLABE format: " + maskClabe(clabe),
            "R01",
            null,
            SpeiOperation.VALIDATION,
            400,
            false
        );
    }

    /**
     * CLABE does not exist or is closed.
     * Banxico code: R03
     */
    public static SpeiPaymentException clabeNotFound(String clabe) {
        return new SpeiPaymentException(
            "CLABE not found or closed: " + maskClabe(clabe),
            "R03",
            null,
            SpeiOperation.VALIDATION,
            404,
            false
        );
    }

    /**
     * Amount exceeds SPEI limit (MXN 800,000 for same-day, higher for scheduled).
     * Banxico code: R14
     */
    public static SpeiPaymentException amountExceedsLimit(BigDecimal amount, BigDecimal limit) {
        return new SpeiPaymentException(
            "Amount " + amount + " exceeds SPEI limit of " + limit,
            "R14",
            null,
            SpeiOperation.TRANSFER,
            422,
            false
        );
    }

    /**
     * Insufficient funds in sender's account.
     * Banxico code: R17
     */
    public static SpeiPaymentException insufficientFunds(String idOrden) {
        return new SpeiPaymentException(
            "Insufficient funds for SPEI order: " + idOrden,
            "R17",
            idOrden,
            SpeiOperation.TRANSFER,
            422,
            false
        );
    }

    /**
     * Transaction rejected by receiving institution.
     * Banxico code: R31
     */
    public static SpeiPaymentException rejectedByReceivingBank(String idOrden, String reason) {
        return new SpeiPaymentException(
            "SPEI order rejected by receiving bank: " + reason,
            "R31",
            idOrden,
            SpeiOperation.TRANSFER,
            422,
            false
        );
    }

    /**
     * Duplicate order detected (same claveRastreo within 24h).
     * Banxico code: R32
     */
    public static SpeiPaymentException duplicateOrder(String claveRastreo) {
        return new SpeiPaymentException(
            "Duplicate SPEI order with claveRastreo: " + claveRastreo,
            "R32",
            null,
            SpeiOperation.TRANSFER,
            409,
            false
        );
    }

    /**
     * Transaction timeout at Banxico/SPEI.
     * Banxico code: T01
     */
    public static SpeiPaymentException transactionTimeout(String idOrden) {
        return new SpeiPaymentException(
            "SPEI transaction timeout: " + idOrden,
            "T01",
            idOrden,
            SpeiOperation.TRANSFER,
            504,
            true
        );
    }

    /**
     * SPEI system unavailable (maintenance window).
     * Banxico code: S01
     */
    public static SpeiPaymentException systemUnavailable(Throwable cause) {
        return new SpeiPaymentException(
            "SPEI system temporarily unavailable",
            "S01",
            null,
            SpeiOperation.TRANSFER,
            503,
            true,
            cause
        );
    }

    /**
     * CoDi QR code generation failed.
     * Banxico code: C01
     */
    public static SpeiPaymentException codiGenerationFailed(String reason) {
        return new SpeiPaymentException(
            "CoDi QR generation failed: " + reason,
            "C01",
            null,
            SpeiOperation.CODI_GENERATION,
            500,
            false
        );
    }

    /**
     * Invalid RFC/CURP format.
     * Banxico code: R40
     */
    public static SpeiPaymentException invalidRfcCurp(String rfcCurp) {
        return new SpeiPaymentException(
            "Invalid RFC/CURP format: " + rfcCurp,
            "R40",
            null,
            SpeiOperation.VALIDATION,
            400,
            false
        );
    }

    /**
     * Order already settled (cannot cancel).
     * Banxico code: R50
     */
    public static SpeiPaymentException alreadySettled(String idOrden) {
        return new SpeiPaymentException(
            "SPEI order already settled and cannot be cancelled: " + idOrden,
            "R50",
            idOrden,
            SpeiOperation.CANCELLATION,
            422,
            false
        );
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private static boolean isRetryableBanxicoCode(String banxicoErrorCode) {
        if (banxicoErrorCode == null) return false;
        return switch (banxicoErrorCode) {
            case "T01", "S01", "S02", "G01", "G02" -> true;
            default -> false;
        };
    }

    private static String maskClabe(String clabe) {
        if (clabe == null || clabe.length() < 6) return "***";
        return clabe.substring(0, 3) + "**********" + clabe.substring(clabe.length() - 3);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getBanxicoErrorCode() {
        return banxicoErrorCode;
    }

    public String getSpeiOrderId() {
        return speiOrderId;
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
                ", speiOrderId='" + speiOrderId + '\'' +
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
        VALIDATION,      // CLABE/RFC validation
        TRANSFER,        // SPEI transfer (pacs.008)
        CODI_GENERATION, // CoDi QR code generation
        CANCELLATION,    // Order cancellation
        REFUND,          // Return / devolución
        RECONCILIATION   // End-of-day reconciliation
    }
}