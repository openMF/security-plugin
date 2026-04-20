/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.spei;

import community.mifos.payments.providers.base.AbstractPaymentProvider;
import community.mifos.payments.providers.base.PaymentFeature;
import community.mifos.payments.core.domain.*;
import community.mifos.payments.infrastructure.audit.PaymentAuditLogger;
import community.mifos.payments.providers.spei.client.dto.SpeiTransferRequest;
import community.mifos.payments.providers.spei.client.dto.SpeiTransferResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;

/**
 * SPEI (Mexico) - Sistema de Pagos Electrónicos Interbancarios
 * Operated by Banco de México. Supports CLABE accounts, CoDi QR, and real-time transfers.
 */
@Component
public class SpeiPaymentProvider extends AbstractPaymentProvider {
    
    private final SpeiConfig config;
    private final RestTemplate restTemplate;
    private final SpeiReconciliationService reconciliationService;
    
    // CLABE: 18-digit standardized bank account number
    private static final Pattern CLABE_PATTERN = Pattern.compile("^\\d{18}$");
    private static final BigDecimal SPEI_MAX_AMOUNT = new BigDecimal("10000000.00"); // MXN 10M
    
    public SpeiPaymentProvider(PaymentAuditLogger auditLogger,
                              SpeiConfig config,
                              SpeiReconciliationService reconciliationService) {
        super(auditLogger);
        this.config = config;
        this.restTemplate = new RestTemplate();
        this.reconciliationService = reconciliationService;
    }
    
    @Override
    public String getProviderCode() { return "SPEI"; }
    
    @Override
    public String getCountryCode() { return "MX"; }
    
    @Override
    public String getCurrency() { return "MXN"; }
    
    @Override
    public boolean supportsFeature(PaymentFeature feature) {
        return switch (feature) {
            case QR_CODE_PAYMENT -> true; // CoDi support
            case WEBHOOK_NOTIFICATIONS -> true;
            case INSTANT_REFUND -> false; // Requires separate SPEI return
            case RECURRING_PAYMENTS, SCHEDULED_PAYMENTS -> false;
            case OFFLINE_CAPABLE -> false;
        };
    }
    
    @Override
    protected void preProcessPayment(FastPayment payment) {
        if (payment.getAmount().compareTo(SPEI_MAX_AMOUNT) > 0) {
            throw new SpeiPaymentException("SPEI amount exceeds MXN 10,000,000.00 limit");
        }
        
        // Validate CLABE checksum
        String clabe = payment.getRecipientIdentifier();
        if (!validateClabeChecksum(clabe)) {
            throw new SpeiPaymentException("Invalid CLABE checksum");
        }
    }
    
    @Override
    protected PaymentTransaction executePayment(FastPayment payment) {
        SpeiTransferRequest request = new SpeiTransferRequest(
            generateTransactionReference(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getRecipientIdentifier(), // CLABE
            payment.getRecipientName(),
            payment.getDescription(),
            config.getInstitutionCode()
        );
        
        // SPEI uses ISO 20022 message format (pacs.008)
        ResponseEntity<SpeiTransferResponse> response = restTemplate.postForEntity(
            config.getBaseUrl() + "/spei/v1/ordenes",
            request,
            SpeiTransferResponse.class
        );
        
        SpeiTransferResponse speiResponse = response.getBody();
        
        return PaymentTransaction.builder()
            .transactionId(speiResponse.getIdOrden())
            .referenceCode(speiResponse.getReferenciaNumerica())
            .providerCode(getProviderCode())
            .amount(payment.getAmount())
            .currency(getCurrency())
            .status(mapSpeiStatus(speiResponse.getEstado()))
            .recipientIdentifier(payment.getRecipientIdentifier())
            .recipientBankCode(extractBankCode(payment.getRecipientIdentifier()))
            .createdAtLocal(LocalDateTime.now())
            .settlementTime(LocalDateTime.now()) // SPEI is real-time
            .build();
    }
    
    @Override
    protected void postProcessPayment(PaymentTransaction transaction) {
        // SPEI settlements are immediate, but we queue for reconciliation
        reconciliationService.queueForReconciliation(transaction);
    }
    
    @Override
    public Optional<PaymentTransaction> queryTransactionStatus(String transactionId) {
        ResponseEntity<SpeiTransferResponse> response = restTemplate.getForEntity(
            config.getBaseUrl() + "/spei/v1/ordenes/" + transactionId,
            SpeiTransferResponse.class
        );
        
        return Optional.ofNullable(response.getBody())
            .map(this::mapToTransaction);
    }
    
    @Override
    public boolean validateRecipient(String recipientIdentifier) {
        if (!CLABE_PATTERN.matcher(recipientIdentifier).matches()) {
            return false;
        }
        return validateClabeChecksum(recipientIdentifier);
    }
    
    @Override
    public String generatePaymentToken(BigDecimal amount, String description) {
        // Generate CoDi QR code (if CoDi enabled)
        if (config.isCodiEnabled()) {
            return generateCodiQr(amount, description);
        }
        // Otherwise return CLABE for manual transfer
        return config.getClabeAccount();
    }
    
    @Override
    public boolean cancelTransaction(String transactionId) {
        // SPEI transactions are final once settled
        // Can only cancel if pending (within same business day, before cut-off)
        ResponseEntity<SpeiTransferResponse> response = restTemplate.exchange(
            config.getBaseUrl() + "/spei/v1/ordenes/" + transactionId,
            org.springframework.http.HttpMethod.DELETE,
            null,
            SpeiTransferResponse.class
        );
        return response.getStatusCode().is2xxSuccessful();
    }
    
    private boolean validateClabeChecksum(String clabe) {
        if (clabe == null || clabe.length() != 18) return false;
        
        int[] weights = {3, 7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1, 3, 7, 1, 3, 7};
        int sum = 0;
        
        for (int i = 0; i < 17; i++) {
            sum += (clabe.charAt(i) - '0') * weights[i];
        }
        
        int checksum = (10 - (sum % 10)) % 10;
        return checksum == (clabe.charAt(17) - '0');
    }
    
    private String extractBankCode(String clabe) {
        return clabe.substring(0, 3); // First 3 digits are bank code
    }
    
    private PaymentStatus mapSpeiStatus(String estado) {
        return switch (estado.toUpperCase()) {
            case "PENDIENTE", "REGISTRADA" -> PaymentStatus.PENDING;
            case "LIQUIDADA", "COMPLETADA" -> PaymentStatus.COMPLETED;
            case "RECHAZADA", "CANCELADA" -> PaymentStatus.FAILED;
            case "DEVUELTA" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.UNKNOWN;
        };
    }
    
    private String generateCodiQr(BigDecimal amount, String description) {
        // CoDi QR generation logic (Banco de México specification)
        return "codi|" + config.getClabeAccount() + "|" + amount + "|" + description;
    }
}