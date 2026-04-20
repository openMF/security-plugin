/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.infrastructure.audit;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Getter
@Table(name = "payment_audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 50)
    private String eventId;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private AuditAction action;

    @Column(name = "provider_code", length = 10)
    private String providerCode;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "transaction_id", length = 50)
    private String transactionId;

    @Column(name = "reference_code", length = 50)
    private String referenceCode;

    @Column(name = "amount", precision = 19, scale = 6)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "recipient_identifier", length = 100)
    private String recipientIdentifier;

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Column(name = "source_account_id")
    private Long sourceAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "old_value", length = 50)
    private String oldValue;

    @Column(name = "new_value", length = 50)
    private String newValue;

    @Column(name = "status_reason", length = 255)
    private String statusReason;

    @Column(name = "details", length = 500)
    private String details;

    @Column(name = "context_json", columnDefinition = "TEXT")
    private String contextJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 10)
    private AuditSeverity severity = AuditSeverity.INFO;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "correlation_id", length = 50)
    private String correlationId;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    // Lombok @Data or manual getters/setters
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final AuditEvent event = new AuditEvent();
        public Builder eventId(String id) { event.eventId = id; return this; }
        public Builder timestamp(LocalDateTime ts) { event.timestamp = ts; return this; }
        public Builder action(AuditAction a) { event.action = a; return this; }
        public Builder providerCode(String p) { event.providerCode = p; return this; }
        public Builder countryCode(String c) { event.countryCode = c; return this; }
        public Builder transactionId(String t) { event.transactionId = t; return this; }
        public Builder referenceCode(String r) { event.referenceCode = r; return this; }
        public Builder amount(BigDecimal a) { event.amount = a; return this; }
        public Builder currency(String c) { event.currency = c; return this; }
        public Builder recipientIdentifier(String r) { event.recipientIdentifier = r; return this; }
        public Builder recipientName(String r) { event.recipientName = r; return this; }
        public Builder sourceAccountId(Long s) { event.sourceAccountId = s; return this; }
        public Builder status(String s) { event.status = s; return this; }
        public Builder oldValue(String o) { event.oldValue = o; return this; }
        public Builder newValue(String n) { event.newValue = n; return this; }
        public Builder statusReason(String r) { event.statusReason = r; return this; }
        public Builder details(String d) { event.details = d; return this; }
        public Builder contextJson(String c) { event.contextJson = c; return this; }
        public Builder severity(AuditSeverity s) { event.severity = s; return this; }
        public Builder success(Boolean s) { event.success = s; return this; }
        public Builder correlationId(String c) { event.correlationId = c; return this; }
        public Builder clientIp(String c) { event.clientIp = c; return this; }
        public Builder userAgent(String u) { event.userAgent = u; return this; }
        public AuditEvent build() { return event; }
    }


}