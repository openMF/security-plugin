/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.core.domain;

/**
 * Payment transaction lifecycle states.
 * Aligned with PIX (Brazil), SPEI (Mexico), and SINPE (Costa Rica) status models.
 * 
 * Uses EclipseLink ObjectTypeConverter for database persistence.
 */
public enum PaymentStatus {
    
    /**
     * Transaction created but not yet submitted to provider.
     * Initial state for all payments.
     */
    PENDING("PENDING"),
    
    /**
     * Transaction submitted to provider, awaiting confirmation.
     * Intermediate state during provider processing.
     */
    PROCESSING("PROCESSING"),
    
    /**
     * Transaction successfully completed and settled.
     * Final successful state.
     */
    COMPLETED("COMPLETED"),
    
    /**
     * Transaction failed at provider level (insufficient funds, invalid account, etc.).
     * Final failure state.
     */
    FAILED("FAILED"),
    
    /**
     * Transaction rejected by risk engine, validation rules, or business logic.
     * Final rejection state (before provider submission).
     */
    REJECTED("REJECTED"),
    
    /**
     * Transaction cancelled by user or system before completion.
     * Final cancelled state.
     */
    CANCELLED("CANCELLED"),
    
    /**
     * Full refund processed after successful completion.
     * Final refunded state.
     */
    REFUNDED("REFUNDED"),
    
    /**
     * Partial refund processed after successful completion.
     * Final partially refunded state.
     */
    PARTIALLY_REFUNDED("PARTIALLY_REFUNDED"),
    
    /**
     * Transaction under dispute or chargeback investigation.
     * Intermediate state pending resolution.
     */
    DISPUTED("DISPUTED"),
    
    /**
     * Transaction expired before completion (e.g., PIX Cobrança 24h window).
     * Final expired state.
     */
    EXPIRED("EXPIRED"),
    
    /**
     * Unknown or unrecognized status from provider.
     * Used when provider returns an unmapped status code.
     */
    UNKNOWN("UNKNOWN");
    
    private final String databaseValue;
    
    PaymentStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }
    
    /**
     * Returns the string value stored in the database.
     * Used by EclipseLink ObjectTypeConverter.
     */
    public String getDatabaseValue() {
        return databaseValue;
    }
    
    /**
     * Checks if this status represents a terminal/final state.
     */
    public boolean isTerminal() {
        return switch (this) {
            case COMPLETED, FAILED, REJECTED, CANCELLED, REFUNDED, 
                 PARTIALLY_REFUNDED, EXPIRED, UNKNOWN -> true;
            case PENDING, PROCESSING, DISPUTED -> false;
        };
    }
    
    /**
     * Checks if this status allows cancellation.
     */
    public boolean isCancellable() {
        return this == PENDING || this == PROCESSING;
    }
    
    /**
     * Checks if this status represents a successful completion.
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }
    
    /**
     * Checks if this status allows refund operations.
     */
    public boolean isRefundable() {
        return this == COMPLETED;
    }
    
    /**
     * Checks if this status requires reconciliation.
     */
    public boolean requiresReconciliation() {
        return this == COMPLETED || this == REFUNDED || this == PARTIALLY_REFUNDED;
    }
    
    /**
     * Checks if this status represents a failure or rejection.
     */
    public boolean isFailure() {
        return this == FAILED || this == REJECTED || this == EXPIRED;
    }
    
    /**
     * Checks if this status is unknown/unrecognized.
     */
    public boolean isUnknown() {
        return this == UNKNOWN;
    }
    
    /**
     * Lookup by database value.
     * Returns UNKNOWN instead of throwing for unrecognized values.
     */
    public static PaymentStatus fromDatabaseValue(String value) {
        if (value == null) {
            return null;
        }
        for (PaymentStatus status : values()) {
            if (status.databaseValue.equals(value)) {
                return status;
            }
        }
        // Return UNKNOWN instead of throwing exception
        return UNKNOWN;
    }
    
    /**
     * Safe lookup by database value with strict mode option.
     * 
     * @param value the database value
     * @param strict if true, throws exception for unknown values; if false, returns UNKNOWN
     * @return the PaymentStatus or UNKNOWN/exception
     */
    public static PaymentStatus fromDatabaseValue(String value, boolean strict) {
        if (value == null) {
            return null;
        }
        for (PaymentStatus status : values()) {
            if (status.databaseValue.equals(value)) {
                return status;
            }
        }
        if (strict) {
            throw new IllegalArgumentException("Unknown PaymentStatus: " + value);
        }
        return UNKNOWN;
    }
}