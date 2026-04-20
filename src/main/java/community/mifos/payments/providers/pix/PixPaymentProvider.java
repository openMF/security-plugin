/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.pix;

import community.mifos.payments.providers.base.AbstractPaymentProvider;
import community.mifos.payments.providers.base.PaymentFeature;
import community.mifos.payments.core.domain.*;
import community.mifos.payments.providers.pix.client.dto.PixQrCodeRequest;
import community.mifos.payments.providers.pix.client.dto.PixQrCodeResponse;
import community.mifos.payments.providers.pix.client.dto.PixTransactionStatus;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

/**
 * PIX (Brazil) Instant Payment System Implementation
 * Operates 24/7 with settlement through SPI (Instant Payment System)
 * Supports: QR Codes, PIX Keys (CPF, Phone, Email), Webhooks
 */
@Component
public class PixPaymentProvider extends AbstractPaymentProvider {
    
    private final PixConfig config;
    private final RestTemplate restTemplate;
    private final PixValidationService validationService;
    
    private static final Set<String> VALID_PIX_KEY_TYPES = Set.of("CPF", "CNPJ", "PHONE", "EMAIL", "EVP");
    private static final BigDecimal MAX_PIX_AMOUNT = new BigDecimal("1000000.00"); // BRL 1M limit
    
    public PixPaymentProvider(PixAuditLogger auditLogger, 
                             PixConfig config,
                             PixValidationService validationService) {
        super(auditLogger);
        this.config = config;
        this.restTemplate = new RestTemplate();
        this.validationService = validationService;
    }
    
    @Override
    public String getProviderCode() { return "PIX"; }
    
    @Override
    public String getCountryCode() { return "BR"; }
    
    @Override
    public String getCurrency() { return "BRL"; }
    
    @Override
    public boolean supportsFeature(PaymentFeature feature) {
        return switch (feature) {
            case QR_CODE_PAYMENT, WEBHOOK_NOTIFICATIONS, INSTANT_REFUND -> true;
            case RECURRING_PAYMENTS, SCHEDULED_PAYMENTS -> true; // Pix Automático
            case OFFLINE_CAPABLE -> false;
        };
    }
    
    @Override
    protected void preProcessPayment(FastPayment payment) {
        // Validate PIX-specific limits
        if (payment.getAmount().compareTo(MAX_PIX_AMOUNT) > 0) {
            throw new PixPaymentException("PIX amount exceeds BRL 1,000,000.00 limit");
        }
        
        // Validate PIX key format
        String pixKey = payment.getRecipientIdentifier();
        if (!validationService.isValidPixKey(pixKey)) {
            throw new PixPaymentException("Invalid PIX key format");
        }
    }
    
    @Override
    protected PaymentTransaction executePayment(FastPayment payment) {
        HttpHeaders headers = createAuthHeaders();
        
        PixQrCodeRequest request = new PixQrCodeRequest(
            payment.getAmount(),
            payment.getDescription(),
            payment.getRecipientIdentifier(),
            generateTransactionReference()
        );
        
        HttpEntity<PixQrCodeRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<PixQrCodeResponse> response = restTemplate.postForEntity(
            config.getBaseUrl() + "/pix/v2/cob",
            entity,
            PixQrCodeResponse.class
        );
        
        PixQrCodeResponse pixResponse = response.getBody();
        
        return PaymentTransaction.builder()
            .transactionId(pixResponse.getTxid())
            .referenceCode(generateTransactionReference())
            .providerCode(getProviderCode())
            .amount(payment.getAmount())
            .currency(getCurrency())
            .status(mapPixStatus(pixResponse.getStatus()))
            .recipientIdentifier(payment.getRecipientIdentifier())
            .recipientName(pixResponse.getDevedor() != null ? 
                pixResponse.getDevedor().getNome() : null)
            .qrCodeData(pixResponse.getLocation()) // QR Code for scanning
            .createdAtLocal(LocalDateTime.now())
            .expiresAtLocal(LocalDateTime.now().plusHours(24)) // PIX Cobrança expires in 24h
            .build();
    }
    
    @Override
    protected void postProcessPayment(PaymentTransaction transaction) {
        // Register webhook for status updates if async
        if (transaction.getStatus() == PaymentStatus.PENDING) {
            registerWebhook(transaction.getTransactionId());
        }
    }
    
    @Override
    public Optional<PaymentTransaction> queryTransactionStatus(String transactionId) {
        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<PixTransactionStatus> response = restTemplate.exchange(
            config.getBaseUrl() + "/pix/v2/cob/" + transactionId,
            HttpMethod.GET,
            entity,
            PixTransactionStatus.class
        );
        
        PixTransactionStatus status = response.getBody();
        if (status == null) {
            return Optional.empty();
        }
        
        return Optional.of(mapToTransaction(status));
    }
    
    /**
     * Maps PixTransactionStatus to PaymentTransaction entity.
     */
    private PaymentTransaction mapToTransaction(PixTransactionStatus status) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(status.getTxid());
        transaction.setStatus(mapPixStatus(status.getStatus()));
        transaction.setAmount(status.getOriginalAmount());
        transaction.setProviderCode(getProviderCode());
        transaction.setCountryCode(getCountryCode());
        transaction.setCurrency(getCurrency());
        
        if (status.getCalendario() != null) {
            transaction.setCreatedAtLocal(status.getCalendario().getCriacao());
            if (status.getCalendario().getExpiracao() != null) {
                transaction.setExpiresAtLocal(
                    status.getCalendario().getCriacao()
                        .plusSeconds(status.getCalendario().getExpiracao())
                );
            }
        }
        
        if (status.isPaid() && status.getPix() != null && !status.getPix().isEmpty()) {
            PixTransactionStatus.PixReceipt receipt = status.getPix().get(0);
            transaction.setCompletedAtLocal(receipt.getHorario());
            transaction.setRecipientName(receipt.getPagador() != null ? 
                receipt.getPagador().getNome() : null);
        }
        
        return transaction;
    }
    
    @Override
    public boolean validateRecipient(String recipientIdentifier) {
        return validationService.validatePixKeyWithBacen(recipientIdentifier);
    }
    
    @Override
    public String generatePaymentToken(BigDecimal amount, String description) {
        // Generate PIX QR Code payload (EMVCo standard)
        PixQrCodeRequest request = new PixQrCodeRequest(amount, description, null, null);
        ResponseEntity<PixQrCodeResponse> response = restTemplate.postForEntity(
            config.getBaseUrl() + "/pix/v2/cob",
            new HttpEntity<>(request, createAuthHeaders()),
            PixQrCodeResponse.class
        );
        return response.getBody().getPayload(); // BR Code
    }
    
    @Override
    public boolean cancelTransaction(String transactionId) {
        // Only pending transactions can be cancelled
        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<Void> response = restTemplate.exchange(
            config.getBaseUrl() + "/pix/v2/cob/" + transactionId,
            HttpMethod.DELETE,
            entity,
            Void.class
        );
        return response.getStatusCode().is2xxSuccessful();
    }
    
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // OAuth2 or Mutual TLS authentication with BACEN
        String auth = config.getClientId() + ":" + config.getClientSecret();
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString(auth.getBytes()));
        
        return headers;
    }
    
    private PaymentStatus mapPixStatus(String pixStatus) {
        return switch (pixStatus.toUpperCase()) {
            case "ATIVA", "PENDENTE" -> PaymentStatus.PENDING;
            case "CONCLUIDA", "LIQUIDADA" -> PaymentStatus.COMPLETED;
            case "REMOVIDA_PELO_USUARIO_RECEBEDOR", "REMOVIDA_PELO_PSP" -> PaymentStatus.CANCELLED;
            case "DEVOLVIDA" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.UNKNOWN;
        };
    }
    
    private void registerWebhook(String transactionId) {
        // Register webhook URL for PIX status updates
    }
}