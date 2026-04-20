/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.infrastructure.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when a payment request fails validation
 * before being submitted to the provider.
 * 
 * Used by PaymentProvider implementations to reject invalid requests
 * early in the flow (fail-fast).
 */
public class InvalidPaymentRequestException extends PaymentException {

    private static final long serialVersionUID = 1L;

    /**
     * Field that failed validation (e.g., "amount", "recipientIdentifier")
     */
    private final String invalidField;

    /**
     * Human-readable validation message
     */
    private final String validationMessage;

    /**
     * Creates exception with generic message.
     */
    public InvalidPaymentRequestException(String message) {
        super(message, "INVALID_REQUEST", null);
        this.invalidField = null;
        this.validationMessage = message;
    }

    /**
     * Creates exception with specific field information.
     */
    public InvalidPaymentRequestException(String invalidField, String validationMessage) {
        super("Invalid payment request: " + validationMessage, "INVALID_REQUEST", null);
        this.invalidField = invalidField;
        this.validationMessage = validationMessage;
    }

    /**
     * Creates exception with field, message, and cause.
     */
    public InvalidPaymentRequestException(String invalidField, String validationMessage, Throwable cause) {
        super("Invalid payment request: " + validationMessage, "INVALID_REQUEST", cause);
        this.invalidField = invalidField;
        this.validationMessage = validationMessage;
    }

    /**
     * Factory method for common validation failures.
     */
    public static InvalidPaymentRequestException invalidAmount(BigDecimal amount) {
        return new InvalidPaymentRequestException(
            "amount",
            "Amount must be greater than zero and less than provider maximum. Provided: " + amount
        );
    }

    public static InvalidPaymentRequestException invalidCurrency(String currency, String expected) {
        return new InvalidPaymentRequestException(
            "currency",
            "Unsupported currency '" + currency + " " + "'. Expected: " + expected
        );
    }

    public static InvalidPaymentRequestException invalidRecipient(String identifier, String provider) {
        return new InvalidPaymentRequestException(
            "recipientIdentifier",
            "Invalid recipient identifier format for " + provider + ": " + identifier
        );
    }

    public static InvalidPaymentRequestException missingRequiredField(String fieldName) {
        return new InvalidPaymentRequestException(
            fieldName,
            "Missing required field: " + fieldName
        );
    }

    public static InvalidPaymentRequestException amountExceedsLimit(BigDecimal amount, BigDecimal maxLimit) {
        return new InvalidPaymentRequestException(
            "amount",
            "Amount " + amount + " exceeds provider limit of " + maxLimit
        );
    }

    public static InvalidPaymentRequestException expiredPaymentWindow(String expirationTime) {
        return new InvalidPaymentRequestException(
            "expiresAt",
            "Payment expiration window has passed: " + expirationTime
        );
    }

    public String getInvalidField() {
        return invalidField;
    }

    public String getValidationMessage() {
        return validationMessage;
    }
}