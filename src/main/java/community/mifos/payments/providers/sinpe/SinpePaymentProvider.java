/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.sinpe;

import community.mifos.payments.providers.base.AbstractPaymentProvider;
import community.mifos.payments.providers.base.PaymentFeature;
import community.mifos.payments.core.domain.*;
import community.mifos.payments.infrastructure.audit.PaymentAuditLogger;
import community.mifos.payments.providers.sinpe.client.dto.SinpeMobileRequest;
import community.mifos.payments.providers.sinpe.client.dto.SinpeMobileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * SINPE Móvil (Costa Rica) - Fast Payment System
 * Uses mobile phone numbers as aliases for bank accounts.
 * Operated by Central Bank of Costa Rica (BCCR).
 */
@Component
public class SinpePaymentProvider extends AbstractPaymentProvider {
    
    private final SinpeConfig config;
    private final RestTemplate restTemplate;
    private final SinpeNotificationService notificationService;
    
    // SINPE Móvil uses phone numbers (8 digits) or IBAN (22 characters)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[25789]\\d{7}$");
    private static final Pattern IBAN_PATTERN = Pattern.compile("^CR\\d{20}$");
    private static final BigDecimal SINPE_MOBILE_LIMIT = new BigDecimal("100000"); // CRC 100,000 (~USD 200)
    
    public SinpePaymentProvider(PaymentAuditLogger auditLogger,
                               SinpeConfig config,
                               SinpeNotificationService notificationService) {
        super(auditLogger);
        this.config = config;
        this.restTemplate = new RestTemplate();
        this.notificationService = notificationService;
    }
    
    @Override
    public String getProviderCode() { return "SINPE"; }
    
    @Override
    public String getCountryCode() { return "CR"; }
    
    @Override
    public String getCurrency() { return "CRC"; } // Only CRC supported for SINPE Móvil
    
    @Override
    public boolean supportsFeature(PaymentFeature feature) {
        return switch (feature) {
            case QR_CODE_PAYMENT -> true;
            case WEBHOOK_NOTIFICATIONS -> true;
            case OFFLINE_CAPABLE -> true; // SMS-based transactions possible
            case INSTANT_REFUND -> false;
            case RECURRING_PAYMENTS, SCHEDULED_PAYMENTS -> false;
        };
    }
    
    @Override
    protected void preProcessPayment(FastPayment payment) {
        String identifier = payment.getRecipientIdentifier();
        
        // Determine if SINPE Móvil (phone) or SINPE (IBAN)
        if (isPhoneNumber(identifier)) {
            // SINPE Móvil limits
            if (payment.getAmount().compareTo(SINPE_MOBILE_LIMIT) > 0) {
                throw new SinpePaymentException("Amount exceeds SINPE Móvil limit of CRC 100,000");
            }
        } else if (!isValidIban(identifier)) {
            throw new SinpePaymentException("Invalid SINPE identifier. Use phone number (8 digits) or IBAN");
        }
        
        // Validate phone number is registered in SINPE
        if (isPhoneNumber(identifier) && !validateSinpeRegistration(identifier)) {
            throw new SinpePaymentException("Phone number not registered in SINPE Móvil");
        }
    }
    
    @Override
    protected PaymentTransaction executePayment(FastPayment payment) {
        boolean isMobile = isPhoneNumber(payment.getRecipientIdentifier());
        
        SinpeMobileRequest request = new SinpeMobileRequest(
            generateTransactionReference(),
            payment.getAmount(),
            isMobile ? "SINPE_MOVIL" : "SINPE",
            payment.getRecipientIdentifier(),
            payment.getDescription(),
            config.getSourcePhone(), // Sender's registered phone
            config.getSourceIban()
        );
        
        ResponseEntity<SinpeMobileResponse> response = restTemplate.postForEntity(
            config.getBaseUrl() + "/sinpe/v1/transferencias",
            request,
            SinpeMobileResponse.class
        );
        
        SinpeMobileResponse sinpeResponse = response.getBody();
        
        return PaymentTransaction.builder()
            .transactionId(sinpeResponse.getIdTransaccion())
            .referenceCode(sinpeResponse.getNumeroComprobante())
            .providerCode(getProviderCode())
            .amount(payment.getAmount())
            .currency(getCurrency())
            .status(mapSinpeStatus(sinpeResponse.getEstado()))
            .recipientIdentifier(payment.getRecipientIdentifier())
            .recipientName(sinpeResponse.getNombreReceptor())
            .channel(isMobile ? "MOBILE" : "IBAN")
            .createdAtLocal(LocalDateTime.now())
            .completedAtLocal(sinpeResponse.getFechaLiquidacion())
            .build();
    }
    
    @Override
    protected void postProcessPayment(PaymentTransaction transaction) {
        // Send SMS notification if mobile transfer
        if ("MOBILE".equals(transaction.getChannel())) {
            notificationService.sendConfirmationSms(transaction);
        }
    }
    
    @Override
    public Optional<PaymentTransaction> queryTransactionStatus(String transactionId) {
        ResponseEntity<SinpeMobileResponse> response = restTemplate.getForEntity(
            config.getBaseUrl() + "/sinpe/v1/transferencias/" + transactionId,
            SinpeMobileResponse.class
        );
        
        SinpeMobileResponse status = response.getBody();
        if (status == null) {
            return Optional.empty();
        }
        
        return Optional.of(mapToTransaction(status));
    }
    
    /**
     * Maps SinpeMobileResponse to PaymentTransaction entity.
     */
    private PaymentTransaction mapToTransaction(SinpeMobileResponse status) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(status.getIdTransaccion());
        transaction.setReferenceCode(status.getNumeroComprobante());
        transaction.setStatus(mapSinpeStatus(status.getEstado()));
        transaction.setAmount(status.getMonto());
        transaction.setCurrency(status.getMoneda() != null ? status.getMoneda() : getCurrency());
        transaction.setProviderCode(getProviderCode());
        transaction.setCountryCode(getCountryCode());
        transaction.setRecipientName(status.getNombreReceptor());
        transaction.setRecipientIdentifier(status.getCuentaCliente());
        transaction.setChannel("SINPE_MOVIL".equals(status.getTipoTransferencia()) ? "MOBILE" : "IBAN");
        
        if (status.getFechaCreacion() != null) {
            transaction.setCreatedAtLocal(status.getFechaCreacion());
        }
        
        if (status.getFechaLiquidacion() != null) {
            transaction.setCompletedAtLocal(status.getFechaLiquidacion());
        }
        
        // Map error details if present
        if (status.hasError()) {
            transaction.setStatusReason(status.getCodigoError() + ": " + status.getMensajeError());
        }
        
        return transaction;
    }
    
    @Override
    public boolean validateRecipient(String recipientIdentifier) {
        if (isPhoneNumber(recipientIdentifier)) {
            return validateSinpeRegistration(recipientIdentifier);
        }
        return isValidIban(recipientIdentifier);
    }
    
    @Override
    public String generatePaymentToken(BigDecimal amount, String description) {
        // Generate SINPE Móvil payment request (similar to QR but text-based)
        return "SINPE|" + config.getSourcePhone() + "|" + amount + "|" + description;
    }
    
    @Override
    public boolean cancelTransaction(String transactionId) {
        // SINPE transactions are generally final
        // Some banks allow cancellation within minutes if not yet processed
        ResponseEntity<Void> response = restTemplate.exchange(
            config.getBaseUrl() + "/sinpe/v1/transferencias/" + transactionId + "/reversar",
            org.springframework.http.HttpMethod.POST,
            null,
            Void.class
        );
        return response.getStatusCode().is2xxSuccessful();
    }
    
    private boolean isPhoneNumber(String identifier) {
        return PHONE_PATTERN.matcher(identifier).matches();
    }
    
    private boolean isValidIban(String identifier) {
        return IBAN_PATTERN.matcher(identifier).matches();
    }
    
    private boolean validateSinpeRegistration(String phoneNumber) {
        // Query BCCR registry for phone number validation
        ResponseEntity<Boolean> response = restTemplate.getForEntity(
            config.getBaseUrl() + "/sinpe/v1/validacion/" + phoneNumber,
            Boolean.class
        );
        return Boolean.TRUE.equals(response.getBody());
    }
    
    private PaymentStatus mapSinpeStatus(String estado) {
        return switch (estado.toUpperCase()) {
            case "PENDIENTE", "PROCESANDO" -> PaymentStatus.PENDING;
            case "COMPLETADA", "LIQUIDADA" -> PaymentStatus.COMPLETED;
            case "FALLIDA", "RECHAZADA" -> PaymentStatus.FAILED;
            case "REVERSADA" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.UNKNOWN;
        };
    }
}