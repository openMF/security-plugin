/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.pix;

import community.mifos.payments.core.domain.PaymentTransaction;
import community.mifos.payments.infrastructure.audit.AuditEvent;
import community.mifos.payments.infrastructure.audit.AuditEventRepository;
import community.mifos.payments.infrastructure.audit.PaymentAuditLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import community.mifos.payments.core.domain.PaymentStatus;
import static community.mifos.payments.core.domain.PaymentStatus.CANCELLED;
import static community.mifos.payments.core.domain.PaymentStatus.COMPLETED;
import static community.mifos.payments.core.domain.PaymentStatus.DISPUTED;
import static community.mifos.payments.core.domain.PaymentStatus.EXPIRED;
import static community.mifos.payments.core.domain.PaymentStatus.FAILED;
import static community.mifos.payments.core.domain.PaymentStatus.PARTIALLY_REFUNDED;
import static community.mifos.payments.core.domain.PaymentStatus.PENDING;
import static community.mifos.payments.core.domain.PaymentStatus.PROCESSING;
import static community.mifos.payments.core.domain.PaymentStatus.REFUNDED;
import static community.mifos.payments.core.domain.PaymentStatus.REJECTED;
import community.mifos.payments.infrastructure.audit.AuditAction;
import community.mifos.payments.infrastructure.audit.AuditSeverity;
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
 * PIX-specific audit logger implementing BACEN (Banco Central do Brasil) 
 * regulatory requirements for logging.
 * Extends the base PaymentAuditLogger with PIX-specific PII masking.
 */
@Component
public class PixAuditLogger extends PaymentAuditLogger {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("PIX_AUDIT");
    private static final Logger SECURITY_LOG = LoggerFactory.getLogger("PIX_SECURITY");

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public PixAuditLogger(AuditEventRepository auditEventRepository) {
        super(auditEventRepository);
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Logs PIX payment initiation with BACEN-specific fields.
     * Overrides parent to add PIX key type detection and masking.
     */
    @Override
    @Async("auditExecutor")
    public CompletableFuture<Void> logPaymentInitiated(PaymentTransaction transaction) {
        return CompletableFuture.runAsync(() -> {
            try {
                
                MDC.put("traceId", transaction.getReferenceCode());
                MDC.put("provider", transaction.getProviderCode());
                MDC.put("action", "PIX_PAYMENT_INITIATED");
                MDC.put("pixKeyType", detectPixKeyType(transaction.getRecipientIdentifier()).name());

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
                    .recipientIdentifier(maskPixKey(transaction.getRecipientIdentifier()))
                    .recipientName(maskName(transaction.getRecipientName()))
                    .sourceAccountId(transaction.getSourceAccountId())
                    .status(transaction.getStatus().name())
                    .correlationId(MDC.get("traceId"))
                    .clientIp(MDC.get("clientIp"))
                    .userAgent(MDC.get("userAgent"))
                    // PIX-specific: log key type without exposing the key
                    .contextJson("{\"pixKeyType\":\"" + detectPixKeyType(transaction.getRecipientIdentifier()) + "\"}")
                    .build();

                AUDIT_LOG.info("PIX payment initiated: {}", toJson(event));
                persistAuditEvent(event);

            } finally {
                MDC.clear();
            }
        });
    }

    /**
     * Logs PIX status changes with BACEN status mapping.
     */
    @Override
    @Async("auditExecutor")
    public CompletableFuture<Void> logStatusChange(String transactionId,
                                                    String providerCode,
                                                    Object oldStatus,
                                                    Object newStatus,
                                                    String reason) {
        return CompletableFuture.runAsync(() -> {
            try {
                String bacenOldStatus = mapToBacenStatus(oldStatus);
                String bacenNewStatus = mapToBacenStatus(newStatus);

                MDC.put("traceId", transactionId);
                MDC.put("provider", providerCode);
                MDC.put("action", "PIX_STATUS_CHANGED");

                AuditEvent event = AuditEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .action(AuditAction.STATUS_CHANGED)
                    .providerCode(providerCode)
                    .transactionId(transactionId)
                    .oldValue(bacenOldStatus)
                    .newValue(bacenNewStatus)
                    .statusReason(reason)
                    .build();

                AUDIT_LOG.info("PIX status changed from {} to {}: {}", 
                    bacenOldStatus, bacenNewStatus, toJson(event));
                persistAuditEvent(event);

            } finally {
                MDC.clear();
            }
        });
    }

    /**
     * Logs PIX webhook receipt from BACEN.
     */
    @Async("auditExecutor")
    public CompletableFuture<Void> logWebhookReceived(String transactionId,
                                                     String eventType,
                                                     String payload) {
        return CompletableFuture.runAsync(() -> {
            try {
                MDC.put("traceId", transactionId);
                MDC.put("provider", "PIX");
                MDC.put("action", "PIX_WEBHOOK_RECEIVED");

                AuditEvent event = AuditEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .action(AuditAction.WEBHOOK_RECEIVED)
                    .providerCode("PIX")
                    .transactionId(transactionId)
                    .details("Webhook received: " + eventType)
                    .contextJson(payload)
                    .build();

                SECURITY_LOG.info("PIX webhook received for {}: {}", transactionId, eventType);
                persistAuditEvent(event);

            } finally {
                MDC.clear();
            }
        });
    }

    /**
     * Logs security events for any provider (PIX, SPEI, SINPE).
     * Generic signature to support cross-provider usage.
     */
    @Async("auditExecutor")
    public CompletableFuture<Void> logSecurityEvent(String transactionId,
                                                     String providerCode,
                                                     String eventType,
                                                     String details,
                                                     Map<String, Object> context) {
        return CompletableFuture.runAsync(() -> {
            try {
                MDC.put("traceId", transactionId);
                MDC.put("provider", providerCode);
                MDC.put("action", eventType);

                AuditEvent event = AuditEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .action(AuditAction.SECURITY_EVENT)
                    .providerCode(providerCode)
                    .transactionId(transactionId)
                    .details(details)
                    .contextJson(context != null ? toJson(context) : null)
                    .severity(AuditSeverity.WARNING)
                    .build();

                SECURITY_LOG.warn("{} security event [{}]: {}", providerCode, eventType, toJson(event));
                persistAuditEvent(event);

            } finally {
                MDC.clear();
            }
        });
    }

    // -------------------------------------------------------------------------
    // PIX-Specific Helpers
    // -------------------------------------------------------------------------

    /**
     * Masks PIX key according to BACEN privacy rules.
     * CPF: ***.***.***-XX
     * CNPJ: XX.XXX.XXX/XXXX-XX
     * Phone: +55 XX XXXXX-XXXX
     * Email: xxx***@domain.com
     * EVP: xxxxxxxx-xxxx-4xxx-8xxx-xxxxxxxxxxxx
     */
    private String maskPixKey(String pixKey) {
        if (pixKey == null || pixKey.isBlank()) return "[EMPTY]";

        PixKeyType type = detectPixKeyType(pixKey);
        
        return switch (type) {
            case CPF -> maskCpf(pixKey);
            case CNPJ -> maskCnpj(pixKey);
            case PHONE -> maskPhone(pixKey);
            case EMAIL -> maskEmail(pixKey);
            case EVP -> maskEvp(pixKey);
            default -> maskGeneric(pixKey);
        };
    }

    private String maskCpf(String cpf) {
        // Format: XXX.XXX.XXX-XX
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9);
        }
        return "***.***.***-" + cpf.substring(cpf.length() - 2);
    }

    private String maskCnpj(String cnpj) {
        // Format: XX.XXX.XXX/XXXX-XX
        if (cnpj.length() == 14) {
            return cnpj.substring(0, 2) + ".***.***/" + 
                   cnpj.substring(8, 12) + "-" + cnpj.substring(12);
        }
        return "**." + cnpj.substring(3, 6) + ".***/****-**";
    }

    private String maskPhone(String phone) {
        // Keep +55 and mask middle digits
        if (phone.startsWith("+55")) {
            return phone.substring(0, 4) + " ****-" + 
                   phone.substring(phone.length() - 4);
        }
        return "**** ****-" + phone.substring(phone.length() - 4);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            String local = email.substring(0, atIndex);
            String domain = email.substring(atIndex + 1);
            return local.substring(0, Math.min(3, local.length())) + 
                   "***@" + domain;
        }
        return "****@" + email.substring(atIndex + 1);
    }

    private String maskEvp(String evp) {
        // UUID: xxxxxxxx-xxxx-4xxx-8xxx-xxxxxxxxxxxx
        return evp.substring(0, 8) + "-****-4***-8***-" + 
               evp.substring(24);
    }

    private String maskGeneric(String value) {
        if (value.length() > 8) {
            return value.substring(0, 4) + "****" + 
                   value.substring(value.length() - 4);
        }
        return "****";
    }

    /**
     * Detects PIX key type for audit logging (without exposing the key).
     */
    private PixKeyType detectPixKeyType(String pixKey) {
        if (pixKey == null) return PixKeyType.UNKNOWN;

        String normalized = pixKey.replaceAll("\\s+", "").toLowerCase();

        if (normalized.matches("^\\d{11}$")) return PixKeyType.CPF;
        if (normalized.matches("^\\d{14}$")) return PixKeyType.CNPJ;
        if (normalized.matches("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")) 
            return PixKeyType.EVP;
        if (normalized.contains("@")) return PixKeyType.EMAIL;
        if (normalized.matches("^\\+?55\\d{10,11}$")) return PixKeyType.PHONE;

        return PixKeyType.UNKNOWN;
    }

    /**
     * Maps internal PaymentStatus to BACEN PIX status strings.
     */
    private String mapToBacenStatus(Object status) {
        if (status instanceof PaymentStatus ps) {
            return switch (ps) {
                case PENDING -> "PENDENTE";
                case PROCESSING -> "EM_PROCESSAMENTO";
                case COMPLETED -> "CONCLUIDA";
                case FAILED -> "ERRO";
                case REJECTED -> "ERRO";
                case CANCELLED -> "CANCELADA";
                case REFUNDED -> "DEVOLVIDA";
                case PARTIALLY_REFUNDED -> "DEVOLVIDA_PARCIAL";
                case DISPUTED -> "EM_DISPUTA";
                case EXPIRED -> "EXPIRADA";
                default -> "DESCONHECIDA";
            };
        }
        return String.valueOf(status);
    }

    private enum PixKeyType {
        CPF, CNPJ, PHONE, EMAIL, EVP, UNKNOWN
    }

    // -------------------------------------------------------------------------
    // JSON Serialization
    // -------------------------------------------------------------------------

    private String toJson(AuditEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            return "{\"error\":\"Serialization failed\"}";
        }
    }
    
    /**
    * Serializes Map to JSON string.
    */
   private String toJson(Map<String, Object> context) {
       try {
           return objectMapper.writeValueAsString(context);
       } catch (Exception e) {
           return "{\"error\":\"Context serialization failed\"}";
       }
   }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    /**
     * Persists audit event in isolated transaction.
     * Uses REQUIRES_NEW to ensure audit trail is maintained even if payment fails.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void persistAuditEvent(AuditEvent event) {
        try {
            auditEventRepository.save(event);
        } catch (Exception ex) {
            // Fail-safe: log to file if DB is unavailable, never throw
            SECURITY_LOG.error(
                "Failed to persist PIX audit event to database [eventId={}]: {}. " +
                "Event: {}", 
                event.getEventId(),
                ex.getMessage(),
                event,
                ex
            );
            
            // Attempt to write to alternative log file
            writeToFailSafeLog(event);
        }
    }

    /**
     * Writes audit event to local fail-safe log file when DB is unavailable.
     */
    private void writeToFailSafeLog(AuditEvent event) {
        try {
            String logEntry = String.format("[%s] AUDIT_FAILSAFE: %s%n",
                java.time.LocalDateTime.now(),
                toJson(event)
            );
            
            java.nio.file.Files.writeString(
                java.nio.file.Path.of("/var/log/mifos/pix-audit-failsafe.log"),
                logEntry,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception ioEx) {
            // Last resort: stderr
            System.err.println("CRITICAL: Failed to write PIX audit failsafe: " + ioEx.getMessage());
            System.err.println("Event: " + event);
        }
    }
    
    private String maskName(String name) {
        if (name == null || name.length() < 3) return "***";
        return name.charAt(0) + "***" + name.charAt(name.length() - 1);
    }
}