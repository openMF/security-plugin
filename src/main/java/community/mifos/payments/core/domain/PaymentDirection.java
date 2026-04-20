/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.core.domain;

/**
 * Direction of the payment relative to the account holder.
 * Used for both internal accounting and provider reconciliation.
 */
public enum PaymentDirection {
    
    /**
     * Outgoing payment from account (default for transfers).
     */
    DEBIT("DEBIT"),
    
    /**
     * Incoming payment to account.
     */
    CREDIT("CREDIT");
    
    private final String databaseValue;
    
    PaymentDirection(String databaseValue) {
        this.databaseValue = databaseValue;
    }
    
    public String getDatabaseValue() {
        return databaseValue;
    }
    
    /**
     * Returns true if this is an outgoing payment.
     */
    public boolean isOutgoing() {
        return this == DEBIT;
    }
    
    /**
     * Returns true if this is an incoming payment.
     */
    public boolean isIncoming() {
        return this == CREDIT;
    }
    
    public static PaymentDirection fromDatabaseValue(String value) {
        if (value == null) {
            return null;
        }
        for (PaymentDirection direction : values()) {
            if (direction.databaseValue.equals(value)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("Unknown PaymentDirection: " + value);
    }
}
