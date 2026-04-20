/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.infrastructure.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import community.mifos.payments.core.domain.PaymentTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Async audit logger for payment operations.
 * Provides structured logging, PII masking, and persistent audit trails.
 * Integrates with Fineract's audit expectations while remaining decoupled.
 */
@Component
public class PaymentAuditLogger {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("PAYMENT_AUDIT");
    private static final Logger SECURITY_LOG = LoggerFactory.getLogger("PAYMENT_SECURITY");

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentAuditLogger(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Logs payment initiation with full context.
     * Async execution ensures payment flow is never blocked.
     */
    @Async("auditExecutor")
    public CompletableFuture<Void> logPaymentInitiated(PaymentTransaction transaction) {
        return CompletableFuture.runAsync(() -> {
            try {
                MDC.put("traceId", transaction.getReferenceCode());
                MDC.put("provider", transaction.getProviderCode());
                MDC.put("action", "PAYMENT_INITIATED");

                AuditEvent event = AuditEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .action(AuditAction.PAYMENT_INITIATED)
                    .providerCode(transaction.getProviderCode())
                    .countryCode(transaction.getCountryCode())
                    .transactionId(transaction.getTransactionId())
                    .referenceCode(transaction.getReferenceCode())
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency())
                    .recipientIdentifier(maskIdentifier(transaction.getRecipientIdentifier(), transaction.getProviderCode()))
                    .recipientName(maskName(transaction.getRecipientName()))
                    .sourceAccountId(transaction.getSourceAccountId())
                    .status(transaction.getStatus().name())
                    .correlationId(MDC.get("traceId"))
                    .clientIp(MDC.get("clientIp"))
                    .userAgent(MDC.get("userAgent"))
                    .build();

                // Structured SLF4J logging for centralized log aggregation (ELK, Splunk, etc.)
                AUDIT_LOG.info("Payment initiated: {}", toJson(event));

                // Persistent database audit trail (isolated transaction)
                persistAuditEvent(event);

            } finally {
                MDC.clear();
            }
        });
    }

    /**
     * Logs status transitions (provider callbacks, reconciliation, etc.)
     */
    @Async("auditExecutor")
    public CompletableFuture<Void> logStatusChange(String transactionId, 
                                                    String providerCode,
                                                    Object oldStatus, 
                                                    Object newStatus,
                                                    String reason) {
        return CompletableFuture.runAsync(() -> {
            try {
                MDC.put("traceId", transactionId);
                MDC.put("provider", providerCode);
                MDC.put("action", "STATUS_CHANGE");

                AuditEvent event = AuditEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .action(AuditAction.STATUS_CHANGED)
                    .providerCode(providerCode)
                    .transactionId(transactionId)
                    .oldValue(String.valueOf(oldStatus))
                    .newValue(String.valueOf(newStatus))
                    .statusReason(reason)
                    .build();

                AUDIT_LOG.info("Payment status changed from {} to {}: {}", oldStatus, newStatus, toJson(event));
                persistAuditEvent(event);

            } finally {
                MDC.clear();
            }
        });
    }

    /**
     * Logs security-relevant events (validation failures, signature errors, etc.)
     */
    @Async("auditExecutor")
    public CompletableFuture<Void> logSecurityEvent(String transactionId,
                                                     String providerCode,
                                                     AuditAction action,
                                                     String details,
                                                     Map<String, Object> context) {
        return CompletableFuture.runAsync(() -> {
            try {
                MDC.put("traceId", transactionId != null ? transactionId : "N/A");
                MDC.put("provider", providerCode != null ? providerCode : "N/A");
                MDC.put("action", action.name());

                AuditEvent event = AuditEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .action(action)
                    .providerCode(providerCode)
                    .transactionId(transactionId)
                    .details(details)
                    .contextJson(context != null ? toJson(context) : null)
                    .severity(AuditSeverity.WARNING)
                    .build();

                SECURITY_LOG.warn("Payment security event [{}]: {}", action, toJson(event));
                persistAuditEvent(event);

            } finally {
                MDC.clear();
            }
        });
    }

    /**
     * Logs provider API calls for debugging and compliance.
     */
    @Async("auditExecutor")
    public CompletableFuture<Void> logProviderCall(String providerCode,
                                                    String endpoint,
                                                    String method,
                                                    int httpStatus,
                                                    long responseTimeMs,
                                                    boolean success) {
        return CompletableFuture.runAsync(() -> {
            AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .action(AuditAction.PROVIDER_API_CALL)
                .providerCode(providerCode)
                .details(String.format("%s %s -> %d (%dms)", method, endpoint, httpStatus, responseTimeMs))
                .success(success)
                .build();

            AUDIT_LOG.info("Provider API call: {}", toJson(event));
            persistAuditEvent(event);
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Persists audit event in isolated transaction so DB failures don't rollback payment.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void persistAuditEvent(AuditEvent event) {
        try {
            auditEventRepository.save(event);
        } catch (Exception ex) {
            // Fail-safe: log to file if DB is unavailable, never throw
            SECURITY_LOG.error("Failed to persist audit event to database: {}", ex.getMessage(), ex);
        }
    }

    /**
     * PII-aware masking for recipient identifiers based on provider type.
     */
    private String maskIdentifier(String identifier, String providerCode) {
        if (identifier == null || identifier.isBlank()) return "[EMPTY]";

        return switch (providerCode) {
            case "PIX" -> maskPixKey(identifier);
            case "SPEI" -> maskClabe(identifier);
            case "SINPE" -> maskSinpeIdentifier(identifier);
            default -> maskGeneric(identifier);
        };
    }

    private String maskPixKey(String key) {
        // PIX Keys: CPF (11), CNPJ (14), Phone (+5511...), Email, EVP UUID
        if (key.contains("@")) {
            String[] parts = key.split("@");
            return parts[0].substring(0, Math.min(2, parts[0].length())) + "***@" + parts[1];
        }
        if (key.length() > 8) {
            return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
        }
        return "****";
    }

    private String maskClabe(String clabe) {
        // CLABE: 18 digits
        if (clabe.length() == 18) {
            return clabe.substring(0, 4) + "**********" + clabe.substring(14);
        }
        return maskGeneric(clabe);
    }

    private String maskSinpeIdentifier(String identifier) {
        // Phone: 8 digits or IBAN: CR + 20 digits
        if (identifier.startsWith("CR") && identifier.length() == 22) {
            return identifier.substring(0, 6) + "************" + identifier.substring(18);
        }
        if (identifier.length() == 8) {
            return identifier.substring(0, 2) + "****" + identifier.substring(6);
        }
        return maskGeneric(identifier);
    }

    private String maskGeneric(String value) {
        if (value.length() <= 4) return "****";
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    private String maskName(String name) {
        if (name == null || name.length() < 3) return "***";
        return name.charAt(0) + "***" + name.charAt(name.length() - 1);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"serialization_error\":\"" + e.getMessage() + "\"}";
        }
    }
}

