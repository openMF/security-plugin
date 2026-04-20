/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.core.domain;

import jakarta.persistence.*;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.ObjectTypeConverter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Core domain entity representing a fast payment transaction.
 * Compatible with EclipseLink (OpenJPA) used by Apache Fineract.
 * 
 * Supports PIX (Brazil), SPEI (Mexico), and SINPE (Costa Rica).
 */
@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_tx_provider", columnList = "provider_code"),
    @Index(name = "idx_tx_status", columnList = "status"),
    @Index(name = "idx_tx_reference", columnList = "reference_code", unique = true),
    @Index(name = "idx_tx_created", columnList = "created_at"),
    @Index(name = "idx_tx_source_account", columnList = "source_account_id"),
    @Index(name = "idx_tx_country_provider", columnList = "country_code,provider_code"),
    @Index(name = "idx_tx_idempotency", columnList = "idempotency_key", unique = true)
})
@ObjectTypeConverter(
    name = "paymentStatusConverter",
    dataType = String.class,
    objectType = PaymentStatus.class,
    conversionValues = {
        @ConversionValue(dataValue = "PENDING", objectValue = "PENDING"),
        @ConversionValue(dataValue = "PROCESSING", objectValue = "PROCESSING"),
        @ConversionValue(dataValue = "COMPLETED", objectValue = "COMPLETED"),
        @ConversionValue(dataValue = "FAILED", objectValue = "FAILED"),
        @ConversionValue(dataValue = "REJECTED", objectValue = "REJECTED"),
        @ConversionValue(dataValue = "CANCELLED", objectValue = "CANCELLED"),
        @ConversionValue(dataValue = "REFUNDED", objectValue = "REFUNDED"),
        @ConversionValue(dataValue = "PARTIALLY_REFUNDED", objectValue = "PARTIALLY_REFUNDED"),
        @ConversionValue(dataValue = "DISPUTED", objectValue = "DISPUTED"),
        @ConversionValue(dataValue = "EXPIRED", objectValue = "EXPIRED"),
        @ConversionValue(dataValue = "UNKNOWN", objectValue = "UNKNOWN")
    }
)
@ObjectTypeConverter(
    name = "paymentDirectionConverter",
    dataType = String.class,
    objectType = PaymentDirection.class,
    conversionValues = {
        @ConversionValue(dataValue = "DEBIT", objectValue = "DEBIT"),
        @ConversionValue(dataValue = "CREDIT", objectValue = "CREDIT")
    }
)
public class PaymentTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    /**
     * Provider's native transaction ID (txid for PIX, idOrden for SPEI, etc.)
     */
    @Column(name = "transaction_id", length = 100, nullable = false)
    private String transactionId;

    /**
     * Internal reference code (PIX-XXXX, SPEI-XXXX, SINPE-XXXX)
     */
    @Column(name = "reference_code", length = 50, nullable = false, unique = true)
    private String referenceCode;

    /**
     * Provider code: PIX, SPEI, SINPE
     */
    @Column(name = "provider_code", length = 10, nullable = false)
    private String providerCode;

    /**
     * ISO country code: BR, MX, CR
     */
    @Column(name = "country_code", length = 2, nullable = false)
    private String countryCode;

    // -------------------------------------------------------------------------
    // Amount Fields
    // -------------------------------------------------------------------------

    @Column(name = "amount", precision = 19, scale = 6, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    /**
     * Amount in USD for reporting/consolidation (optional)
     */
    @Column(name = "amount_usd", precision = 19, scale = 6)
    private BigDecimal amountUsd;

    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    // -------------------------------------------------------------------------
    // Party Information
    // -------------------------------------------------------------------------

    @Column(name = "source_account_id")
    private Long sourceAccountId;

    @Column(name = "source_client_id")
    private Long sourceClientId;

    /**
     * Source identifier (IBAN, account number, etc.)
     */
    @Column(name = "source_identifier", length = 100)
    private String sourceIdentifier;

    /**
     * Recipient identifier (PIX key, CLABE, SINPE phone, IBAN)
     */
    @Column(name = "recipient_identifier", length = 100, nullable = false)
    private String recipientIdentifier;

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    /**
     * Bank code for the recipient (3 digits for SPEI, etc.)
     */
    @Column(name = "recipient_bank_code", length = 10)
    private String recipientBankCode;

    @Column(name = "recipient_bank_name", length = 100)
    private String recipientBankName;

    // -------------------------------------------------------------------------
    // Transaction Details
    // -------------------------------------------------------------------------

    @Column(name = "description", length = 500)
    private String description;

    /**
     * Transaction status using EclipseLink ObjectTypeConverter
     */
    @Convert("paymentStatusConverter")
    @Column(name = "status", length = 20, nullable = false)
    private PaymentStatus status;

    /**
     * Detailed status reason or error message
     */
    @Column(name = "status_reason", length = 500)
    private String statusReason;

    /**
     * Payment direction using EclipseLink ObjectTypeConverter
     */
    @Convert("paymentDirectionConverter")
    @Column(name = "direction", length = 10, nullable = false)
    private PaymentDirection direction = PaymentDirection.DEBIT;

    /**
     * Channel used: MOBILE, WEB, API, BRANCH, ATM
     */
    @Column(name = "channel", length = 20)
    private String channel;

    // -------------------------------------------------------------------------
    // Idempotency & Callback
    // -------------------------------------------------------------------------

    /**
     * Idempotency key to prevent duplicate submissions.
     * Unique across all transactions.
     */
    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    /**
     * Callback URL for async status notifications (overrides default webhook).
     */
    @Column(name = "callback_url", length = 500)
    private String callbackUrl;

    // -------------------------------------------------------------------------
    // Provider-Specific Data
    // -------------------------------------------------------------------------

    /**
     * QR code data (PIX payload, CoDi code, etc.)
     */
    @Column(name = "qr_code_data", length = 2000)
    private String qrCodeData;

    /**
     * URL for payment confirmation/tracking
     */
    @Column(name = "payment_url", length = 500)
    private String paymentUrl;

    /**
     * JSON blob for provider-specific metadata
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "provider_metadata", columnDefinition = "CLOB")
    private String providerMetadata;

    /**
     * Raw request/response logs (encrypted at rest)
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "raw_request", columnDefinition = "CLOB")
    private String rawRequest;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "raw_response", columnDefinition = "CLOB")
    private String rawResponse;

    // -------------------------------------------------------------------------
    // Timing Fields (using java.util.Date for EclipseLink compatibility)
    // -------------------------------------------------------------------------

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;

    /**
     * When the transaction was completed at the provider
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "completed_at")
    private Date completedAt;

    /**
     * When the transaction was settled (funds moved)
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "settled_at")
    private Date settledAt;

    /**
     * Expiration time for pending transactions (PIX Cobrança: 24h)
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expires_at")
    private Date expiresAt;

    // -------------------------------------------------------------------------
    // Reconciliation & Accounting
    // -------------------------------------------------------------------------

    /**
     * Whether the transaction has been reconciled with provider
     */
    @Column(name = "reconciled")
    private Boolean reconciled = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "reconciled_at")
    private Date reconciledAt;

    /**
     * Associated accounting entry ID in Fineract
     */
    @Column(name = "accounting_entry_id")
    private Long accountingEntryId;

    /**
     * Journal entry reference
     */
    @Column(name = "journal_entry_ref", length = 50)
    private String journalEntryRef;

    // -------------------------------------------------------------------------
    // Fees & Charges
    // -------------------------------------------------------------------------

    @Column(name = "fee_amount", precision = 19, scale = 6)
    private BigDecimal feeAmount;

    @Column(name = "fee_currency", length = 3)
    private String feeCurrency;

    @Column(name = "tax_amount", precision = 19, scale = 6)
    private BigDecimal taxAmount;

    // -------------------------------------------------------------------------
    // Refund Fields
    // -------------------------------------------------------------------------

    @Column(name = "original_transaction_id", length = 100)
    private String originalTransactionId;

    @Column(name = "refund_reason", length = 255)
    private String refundReason;

    // -------------------------------------------------------------------------
    // Security & Risk
    // -------------------------------------------------------------------------

    /**
     * Risk score (0-100)
     */
    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "risk_flags", length = 255)
    private String riskFlags;

    /**
     * IP address of the initiator
     */
    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    /**
     * Device fingerprint
     */
    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    // -------------------------------------------------------------------------
    // Webhook Tracking
    // -------------------------------------------------------------------------

    @Column(name = "webhook_delivered")
    private Boolean webhookDelivered = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "webhook_delivered_at")
    private Date webhookDeliveredAt;

    @Column(name = "webhook_attempts")
    private Integer webhookAttempts = 0;

    // -------------------------------------------------------------------------
    // Versioning (Optimistic Locking)
    // -------------------------------------------------------------------------

    @Version
    @Column(name = "version")
    private Long version;

    // -------------------------------------------------------------------------
    // Transient Fields (Not persisted)
    // -------------------------------------------------------------------------

    @Transient
    private Map<String, Object> transientContext = new HashMap<>();

    // -------------------------------------------------------------------------
    // Lifecycle Callbacks
    // -------------------------------------------------------------------------

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        
        // Generate reference code if not provided
        if (this.referenceCode == null && this.providerCode != null) {
            this.referenceCode = this.providerCode + "-" + 
                UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        }
        
        // Default status
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
        
        // Default direction
        if (this.direction == null) {
            this.direction = PaymentDirection.DEBIT;
        }
        
        // Default reconciled
        if (this.reconciled == null) {
            this.reconciled = false;
        }
        
        // Default webhook attempts
        if (this.webhookAttempts == null) {
            this.webhookAttempts = 0;
        }
        
        // Default webhook delivered
        if (this.webhookDelivered == null) {
            this.webhookDelivered = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = new Date();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public PaymentTransaction() {
        // JPA required
    }

    // -------------------------------------------------------------------------
    // Business Methods
    // -------------------------------------------------------------------------

    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }

    public boolean isProcessing() {
        return this.status == PaymentStatus.PROCESSING;
    }

    public boolean isCompleted() {
        return this.status == PaymentStatus.COMPLETED;
    }

    public boolean isFailed() {
        return this.status == PaymentStatus.FAILED || this.status == PaymentStatus.REJECTED;
    }

    public boolean isExpired() {
        return this.expiresAt != null && new Date().after(this.expiresAt);
    }

    public boolean canBeCancelled() {
        return isPending() && !isExpired();
    }

    public boolean isReversible() {
        if (!isCompleted() || this.completedAt == null) {
            return false;
        }
        // Check if within 24 hours
        long completedTime = this.completedAt.getTime();
        long now = System.currentTimeMillis();
        return (now - completedTime) < (24 * 60 * 60 * 1000);
    }

    public void markAsCompleted(LocalDateTime completedAt) {
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = completedAt != null ? 
            Date.from(completedAt.atZone(ZoneId.systemDefault()).toInstant()) : new Date();
    }

    public void markAsCompleted(Date completedAt) {
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = completedAt != null ? completedAt : new Date();
    }

    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.statusReason = reason;
    }

    public void markAsRefunded(String originalTxId, String reason) {
        this.status = PaymentStatus.REFUNDED;
        this.originalTransactionId = originalTxId;
        this.refundReason = reason;
    }

    public void markAsProcessing() {
        this.status = PaymentStatus.PROCESSING;
    }

    public void addTransientContext(String key, Object value) {
        this.transientContext.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getTransientContext(String key) {
        return (T) this.transientContext.get(key);
    }

    // -------------------------------------------------------------------------
    // Helper Methods for Date/Time Conversion
    // -------------------------------------------------------------------------

    public LocalDateTime getCreatedAtLocal() {
        return this.createdAt != null ? 
            LocalDateTime.ofInstant(this.createdAt.toInstant(), ZoneId.systemDefault()) : null;
    }

    public LocalDateTime getUpdatedAtLocal() {
        return this.updatedAt != null ? 
            LocalDateTime.ofInstant(this.updatedAt.toInstant(), ZoneId.systemDefault()) : null;
    }

    public LocalDateTime getCompletedAtLocal() {
        return this.completedAt != null ? 
            LocalDateTime.ofInstant(this.completedAt.toInstant(), ZoneId.systemDefault()) : null;
    }

    public LocalDateTime getSettledAtLocal() {
        return this.settledAt != null ? 
            LocalDateTime.ofInstant(this.settledAt.toInstant(), ZoneId.systemDefault()) : null;
    }

    public LocalDateTime getExpiresAtLocal() {
        return this.expiresAt != null ? 
            LocalDateTime.ofInstant(this.expiresAt.toInstant(), ZoneId.systemDefault()) : null;
    }

    public void setExpiresAtLocal(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt != null ? 
            Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }

    public void setCompletedAtLocal(LocalDateTime completedAt) {
        this.completedAt = completedAt != null ? 
            Date.from(completedAt.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }

    public void setSettledAtLocal(LocalDateTime settledAt) {
        this.settledAt = settledAt != null ? 
            Date.from(settledAt.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }

    // -------------------------------------------------------------------------
    // Equals & HashCode (based on referenceCode - business key)
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentTransaction that = (PaymentTransaction) o;
        return Objects.equals(referenceCode, that.referenceCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceCode);
    }

    @Override
    public String toString() {
        return "PaymentTransaction{" +
                "referenceCode='" + referenceCode + '\'' +
                ", providerCode='" + providerCode + '\'' +
                ", status=" + status +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                '}';
    }

    // -------------------------------------------------------------------------
    // Builder Pattern
    // -------------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final PaymentTransaction tx = new PaymentTransaction();

        public Builder transactionId(String id) {
            tx.transactionId = id;
            return this;
        }

        public Builder referenceCode(String code) {
            tx.referenceCode = code;
            return this;
        }

        public Builder providerCode(String code) {
            tx.providerCode = code;
            return this;
        }

        public Builder countryCode(String code) {
            tx.countryCode = code;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            tx.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            tx.currency = currency;
            return this;
        }

        public Builder amountUsd(BigDecimal amount) {
            tx.amountUsd = amount;
            return this;
        }

        public Builder exchangeRate(BigDecimal rate) {
            tx.exchangeRate = rate;
            return this;
        }

        public Builder sourceAccountId(Long id) {
            tx.sourceAccountId = id;
            return this;
        }

        public Builder sourceClientId(Long id) {
            tx.sourceClientId = id;
            return this;
        }

        public Builder sourceIdentifier(String identifier) {
            tx.sourceIdentifier = identifier;
            return this;
        }

        public Builder recipientIdentifier(String identifier) {
            tx.recipientIdentifier = identifier;
            return this;
        }

        public Builder recipientName(String name) {
            tx.recipientName = name;
            return this;
        }

        public Builder recipientBankCode(String code) {
            tx.recipientBankCode = code;
            return this;
        }

        public Builder recipientBankName(String name) {
            tx.recipientBankName = name;
            return this;
        }

        public Builder description(String description) {
            tx.description = description;
            return this;
        }

        public Builder status(PaymentStatus status) {
            tx.status = status;
            return this;
        }

        public Builder statusReason(String reason) {
            tx.statusReason = reason;
            return this;
        }

        public Builder direction(PaymentDirection direction) {
            tx.direction = direction;
            return this;
        }

        public Builder channel(String channel) {
            tx.channel = channel;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            tx.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder callbackUrl(String callbackUrl) {
            tx.callbackUrl = callbackUrl;
            return this;
        }

        public Builder qrCodeData(String data) {
            tx.qrCodeData = data;
            return this;
        }

        public Builder paymentUrl(String url) {
            tx.paymentUrl = url;
            return this;
        }

        public Builder providerMetadata(String metadata) {
            tx.providerMetadata = metadata;
            return this;
        }

        public Builder rawRequest(String request) {
            tx.rawRequest = request;
            return this;
        }

        public Builder rawResponse(String response) {
            tx.rawResponse = response;
            return this;
        }

        public Builder createdAt(Date createdAt) {
            tx.createdAt = createdAt;
            return this;
        }

        public Builder createdAtLocal(LocalDateTime createdAt) {
            tx.createdAt = createdAt != null ? 
                Date.from(createdAt.atZone(ZoneId.systemDefault()).toInstant()) : null;
            return this;
        }

        public Builder completedAt(Date completedAt) {
            tx.completedAt = completedAt;
            return this;
        }

        public Builder completedAtLocal(LocalDateTime completedAt) {
            tx.completedAt = completedAt != null ? 
                Date.from(completedAt.atZone(ZoneId.systemDefault()).toInstant()) : null;
            return this;
        }

        public Builder settledAt(Date settledAt) {
            tx.settledAt = settledAt;
            return this;
        }

        public Builder settledAtLocal(LocalDateTime settledAt) {
            tx.settledAt = settledAt != null ? 
                Date.from(settledAt.atZone(ZoneId.systemDefault()).toInstant()) : null;
            return this;
        }

        public Builder expiresAt(Date expiresAt) {
            tx.expiresAt = expiresAt;
            return this;
        }

        public Builder expiresAtLocal(LocalDateTime expiresAt) {
            tx.expiresAt = expiresAt != null ? 
                Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()) : null;
            return this;
        }

        public Builder reconciled(Boolean reconciled) {
            tx.reconciled = reconciled;
            return this;
        }

        public Builder reconciledAt(Date reconciledAt) {
            tx.reconciledAt = reconciledAt;
            return this;
        }

        public Builder accountingEntryId(Long id) {
            tx.accountingEntryId = id;
            return this;
        }

        public Builder journalEntryRef(String ref) {
            tx.journalEntryRef = ref;
            return this;
        }

        public Builder feeAmount(BigDecimal amount) {
            tx.feeAmount = amount;
            return this;
        }

        public Builder feeCurrency(String currency) {
            tx.feeCurrency = currency;
            return this;
        }

        public Builder taxAmount(BigDecimal amount) {
            tx.taxAmount = amount;
            return this;
        }

        public Builder originalTransactionId(String id) {
            tx.originalTransactionId = id;
            return this;
        }

        public Builder refundReason(String reason) {
            tx.refundReason = reason;
            return this;
        }

        public Builder riskScore(Integer score) {
            tx.riskScore = score;
            return this;
        }

        public Builder riskFlags(String flags) {
            tx.riskFlags = flags;
            return this;
        }

        public Builder sourceIp(String ip) {
            tx.sourceIp = ip;
            return this;
        }

        public Builder deviceFingerprint(String fingerprint) {
            tx.deviceFingerprint = fingerprint;
            return this;
        }

        public Builder webhookDelivered(Boolean delivered) {
            tx.webhookDelivered = delivered;
            return this;
        }

        public Builder webhookDeliveredAt(Date deliveredAt) {
            tx.webhookDeliveredAt = deliveredAt;
            return this;
        }

        public Builder webhookAttempts(Integer attempts) {
            tx.webhookAttempts = attempts;
            return this;
        }

        public PaymentTransaction build() {
            return tx;
        }
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

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

    public BigDecimal getAmountUsd() {
        return amountUsd;
    }

    public void setAmountUsd(BigDecimal amountUsd) {
        this.amountUsd = amountUsd;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
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

    public String getRecipientBankCode() {
        return recipientBankCode;
    }

    public void setRecipientBankCode(String recipientBankCode) {
        this.recipientBankCode = recipientBankCode;
    }

    public String getRecipientBankName() {
        return recipientBankName;
    }

    public void setRecipientBankName(String recipientBankName) {
        this.recipientBankName = recipientBankName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public PaymentDirection getDirection() {
        return direction;
    }

    public void setDirection(PaymentDirection direction) {
        this.direction = direction;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
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

    public String getQrCodeData() {
        return qrCodeData;
    }

    public void setQrCodeData(String qrCodeData) {
        this.qrCodeData = qrCodeData;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getProviderMetadata() {
        return providerMetadata;
    }

    public void setProviderMetadata(String providerMetadata) {
        this.providerMetadata = providerMetadata;
    }

    public String getRawRequest() {
        return rawRequest;
    }

    public void setRawRequest(String rawRequest) {
        this.rawRequest = rawRequest;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public Date getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(Date settledAt) {
        this.settledAt = settledAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getReconciled() {
        return reconciled;
    }

    public void setReconciled(Boolean reconciled) {
        this.reconciled = reconciled;
    }

    public Date getReconciledAt() {
        return reconciledAt;
    }

    public void setReconciledAt(Date reconciledAt) {
        this.reconciledAt = reconciledAt;
    }

    public Long getAccountingEntryId() {
        return accountingEntryId;
    }

    public void setAccountingEntryId(Long accountingEntryId) {
        this.accountingEntryId = accountingEntryId;
    }

    public String getJournalEntryRef() {
        return journalEntryRef;
    }

    public void setJournalEntryRef(String journalEntryRef) {
        this.journalEntryRef = journalEntryRef;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public String getFeeCurrency() {
        return feeCurrency;
    }

    public void setFeeCurrency(String feeCurrency) {
        this.feeCurrency = feeCurrency;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(String originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskFlags() {
        return riskFlags;
    }

    public void setRiskFlags(String riskFlags) {
        this.riskFlags = riskFlags;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public Boolean getWebhookDelivered() {
        return webhookDelivered;
    }

    public void setWebhookDelivered(Boolean webhookDelivered) {
        this.webhookDelivered = webhookDelivered;
    }

    public Date getWebhookDeliveredAt() {
        return webhookDeliveredAt;
    }

    public void setWebhookDeliveredAt(Date webhookDeliveredAt) {
        this.webhookDeliveredAt = webhookDeliveredAt;
    }

    public Integer getWebhookAttempts() {
        return webhookAttempts;
    }

    public void setWebhookAttempts(Integer webhookAttempts) {
        this.webhookAttempts = webhookAttempts;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public void setCreatedAtLocal(LocalDateTime createdAt) {
        this.createdAt = createdAt != null ? 
            Date.from(createdAt.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }
}