/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.base;

import community.mifos.payments.core.domain.FastPayment;
import community.mifos.payments.core.domain.PaymentTransaction;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Strategy interface for fast payment providers.
 * Implementations: PixPaymentProvider, SpeiPaymentProvider, SinpePaymentProvider
 */
public interface PaymentProvider {
    
    String getProviderCode(); // "PIX", "SPEI", "SINPE"
    String getCountryCode();  // "BR", "MX", "CR"
    String getCurrency();     // "BRL", "MXN", "CRC"
    
    /**
     * Initiate a fast payment transaction
     */
    PaymentTransaction initiatePayment(FastPayment paymentRequest);
    
    /**
     * Query transaction status from provider
     */
    Optional<PaymentTransaction> queryTransactionStatus(String transactionId);
    
    /**
     * Validate recipient identifier (PIX key, CLABE, Phone number)
     */
    boolean validateRecipient(String recipientIdentifier);
    
    /**
     * Generate QR code or payment token for in-person payments
     */
    String generatePaymentToken(BigDecimal amount, String description);
    
    /**
     * Cancel a pending transaction (if supported by provider)
     */
    boolean cancelTransaction(String transactionId);
    
    /**
     * Check if provider supports specific feature
     */
    boolean supportsFeature(PaymentFeature feature);
}

