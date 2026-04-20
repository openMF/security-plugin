/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import community.mifos.payments.core.domain.PaymentStatus;
import community.mifos.payments.core.domain.PaymentTransaction;
import community.mifos.payments.core.repository.PaymentTransactionRepository;
import community.mifos.payments.infrastructure.security.WebhookSignatureValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PixWebhookHandler implements WebhookHandler {
    
    private final PaymentTransactionRepository repository;
    private final WebhookSignatureValidator signatureValidator;
    
    @Override
    public String getProviderCode() { return "PIX"; }
    
    @Override
    public void handleWebhook(JsonNode payload, String signature) {
        // Validate webhook signature (BACEN JWS)
        if (!signatureValidator.validatePixWebhook(payload, signature)) {
            throw new SecurityException("Invalid webhook signature");
        }
        
        String txid = payload.get("txid").asText();
        String status = payload.get("status").asText();
        
        PaymentTransaction transaction = repository.findByTransactionId(txid)
            .orElseThrow(() -> new RuntimeException("Transaction not found: " + txid));
        
        transaction.setStatus(mapPixStatus(status));
        transaction.setProviderMetadata(payload.toString());
        repository.save(transaction);
    }
    
    private PaymentStatus mapPixStatus(String status) {
        return switch (status) {
            case "CONCLUIDA" -> PaymentStatus.COMPLETED;
            case "DEVOLVIDA" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.PENDING;
        };
    }
}