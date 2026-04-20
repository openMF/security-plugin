/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.core.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Command object for initiating a fast payment transfer.
 * Provider-agnostic DTO consumed by the {@link community.mifos.payments.core.service.FastPaymentService}.
 * Supports PIX (Brazil), SPEI (Mexico), and SINPE (Costa Rica).
 */
public class FastPayment {

    /**
     * Transfer amount (e.g., 150.00)
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    /**
     * ISO 4217 currency code (BRL, MXN, CRC)
     */
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency;

    /**
     * Recipient identifier:
     * - PIX: CPF, CNPJ, phone, email, or EVP key
     * - SPEI: 18-digit CLABE
     * - SINPE: 8-digit phone number or IBAN (CRxx...)
     */
    @NotBlank(message = "Recipient identifier is required")
    @Size(max = 100, message = "Recipient identifier must not exceed 100 characters")
    private String recipientIdentifier;

    /**
     * Display name of the recipient (optional, used for confirmation screens)
     */
    @Size(max = 200, message = "Recipient name must not exceed 200 characters")
    private String recipientName;

    /**
     * Transfer description / memo (visible to recipient)
     */
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * Internal Fineract savings/loan account ID (source of funds)
     */
    @NotNull(message = "Source account ID is required")
    private Long sourceAccountId;

    /**
     * Internal Fineract client ID (for audit and limit checks)
     */
    private Long sourceClientId;

    /**
     * Source account identifier (IBAN, account number) if different from sourceAccountId
     */
    @Size(max = 100)
    private String sourceIdentifier;

    /**
     * ISO 3166-1 alpha-2 country code used to auto-resolve provider:
     * BR → PIX, MX → SPEI, CR → SINPE
     */
    @NotBlank(message = "Country code is required")
    @Size(min = 2, max = 2, message = "Country code must be 2 characters")
    private String countryCode;

    /**
     * Optional explicit provider override (PIX, SPEI, SINPE).
     * If omitted, resolved from countryCode.
     */
    @Size(max = 10)
    private String providerCode;

    /**
     * Origination channel: MOBILE, WEB, API, USSD, BRANCH
     */
    @Size(max = 20)
    private String channel = "API";

    /**
     * Requested payment method: DIRECT_TRANSFER, QR_CODE, TOKEN
     */
    @Size(max = 20)
    private String paymentMethod = "DIRECT_TRANSFER";

    /**
     * Idempotency key to prevent duplicate submissions
     */
    @Size(max = 100)
    private String idempotencyKey;

    /**
     * Callback URL for async status notifications (overrides default webhook)
     */
    @Size(max = 500)
    private String callbackUrl;

    /**
     * Provider-specific extra parameters (e.g., PIX expiry hours, SPEI priority)
     */
    private Map<String, String> metadata = new HashMap<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public FastPayment() {
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final FastPayment cmd = new FastPayment();

        public Builder amount(BigDecimal amount) {
            cmd.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            cmd.currency = currency;
            return this;
        }

        public Builder recipientIdentifier(String identifier) {
            cmd.recipientIdentifier = identifier;
            return this;
        }

        public Builder recipientName(String name) {
            cmd.recipientName = name;
            return this;
        }

        public Builder description(String description) {
            cmd.description = description;
            return this;
        }

        public Builder sourceAccountId(Long id) {
            cmd.sourceAccountId = id;
            return this;
        }

        public Builder sourceClientId(Long id) {
            cmd.sourceClientId = id;
            return this;
        }

        public Builder sourceIdentifier(String identifier) {
            cmd.sourceIdentifier = identifier;
            return this;
        }

        public Builder countryCode(String code) {
            cmd.countryCode = code;
            return this;
        }

        public Builder providerCode(String code) {
            cmd.providerCode = code;
            return this;
        }

        public Builder channel(String channel) {
            cmd.channel = channel;
            return this;
        }

        public Builder paymentMethod(String method) {
            cmd.paymentMethod = method;
            return this;
        }

        public Builder idempotencyKey(String key) {
            cmd.idempotencyKey = key;
            return this;
        }

        public Builder callbackUrl(String url) {
            cmd.callbackUrl = url;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            cmd.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            return this;
        }

        public Builder addMetadata(String key, String value) {
            cmd.metadata.put(key, value);
            return this;
        }

        public FastPayment build() {
            return cmd;
        }
    }

    // -------------------------------------------------------------------------
    // Business helpers
    // -------------------------------------------------------------------------

    /**
     * Returns true if this is a QR code generation request rather than an immediate transfer.
     */
    public boolean isQrCodeRequest() {
        return "QR_CODE".equalsIgnoreCase(this.paymentMethod);
    }

    /**
     * Returns true if an explicit provider code was specified.
     */
    public boolean hasExplicitProvider() {
        return this.providerCode != null && !this.providerCode.isBlank();
    }

    /**
     * Returns the effective provider code (explicit or derived from country).
     */
    public String resolveProviderCode() {
        if (hasExplicitProvider()) {
            return this.providerCode.toUpperCase();
        }
        return switch (this.countryCode != null ? this.countryCode.toUpperCase() : "") {
            case "BR" -> "PIX";
            case "MX" -> "SPEI";
            case "CR" -> "SINPE";
            default -> throw new IllegalStateException(
                "Cannot resolve provider for country: " + this.countryCode);
        };
    }

    /**
     * Adds a provider-specific parameter.
     */
    public void addMeta(String key, String value) {
        this.metadata.put(key, value);
    }

    // -------------------------------------------------------------------------
    // Equals / HashCode / ToString
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FastPayment that = (FastPayment) o;
        return Objects.equals(idempotencyKey, that.idempotencyKey)
            && Objects.equals(sourceAccountId, that.sourceAccountId)
            && Objects.equals(recipientIdentifier, that.recipientIdentifier)
            && Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idempotencyKey, sourceAccountId, recipientIdentifier, amount);
    }

    @Override
    public String toString() {
        // Mask recipient identifier for logs
        String maskedRecipient = recipientIdentifier != null && recipientIdentifier.length() > 4
            ? recipientIdentifier.substring(0, 2) + "***" + recipientIdentifier.substring(recipientIdentifier.length() - 2)
            : "***";
        return "FastPayment{" +
            "amount=" + amount +
            ", currency='" + currency + '\'' +
            ", countryCode='" + countryCode + '\'' +
            ", providerCode='" + providerCode + '\'' +
            ", recipientIdentifier='" + maskedRecipient + '\'' +
            ", sourceAccountId=" + sourceAccountId +
            ", paymentMethod='" + paymentMethod + '\'' +
            '}';
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getRecipientIdentifier() {
        return recipientIdentifier;
    }

    public void setRecipientIdentifier(String recipientIdentifier) {
        this.recipientIdentifier = recipientIdentifier;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(Long sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public Long getSourceClientId() {
        return sourceClientId;
    }

    public void setSourceClientId(Long sourceClientId) {
        this.sourceClientId = sourceClientId;
    }

    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
}