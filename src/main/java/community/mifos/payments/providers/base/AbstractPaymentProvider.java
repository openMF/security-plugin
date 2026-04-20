/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.base;

import community.mifos.payments.core.domain.FastPayment;
import community.mifos.payments.core.domain.PaymentTransaction;
import community.mifos.payments.infrastructure.audit.PaymentAuditLogger;
import community.mifos.payments.infrastructure.exception.InvalidPaymentRequestException;
import java.util.UUID;

/**
 * Template method pattern for common payment flows.
 * Provider-specific implementations override hook methods.
 */
public abstract class AbstractPaymentProvider implements PaymentProvider {
    
    protected final PaymentAuditLogger auditLogger;
    
    protected AbstractPaymentProvider(PaymentAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }
    
    @Override
    public final PaymentTransaction initiatePayment(FastPayment payment) {
        // 1. Pre-validation (common)
        validatePaymentRequest(payment);
        
        // 2. Provider-specific pre-processing
        preProcessPayment(payment);
        
        // 3. Execute payment (provider-specific)
        PaymentTransaction transaction = executePayment(payment);
        
        // 4. Post-processing (common)
        postProcessPayment(transaction);
        
        // 5. Audit logging (common)
        auditLogger.logPaymentInitiated(transaction);
        
        return transaction;
    }
    
    // Hook methods for provider-specific implementations
    protected abstract void preProcessPayment(FastPayment payment);
    protected abstract PaymentTransaction executePayment(FastPayment payment);
    protected abstract void postProcessPayment(PaymentTransaction transaction);
    
    // Common validation logic
    private void validatePaymentRequest(FastPayment payment) {
        if (payment.getAmount() == null || payment.getAmount().doubleValue() <= 0) {
            throw new InvalidPaymentRequestException("Invalid payment amount");
        }
        if (!validateRecipient(payment.getRecipientIdentifier())) {
            throw new InvalidPaymentRequestException("Invalid recipient identifier");
        }
    }
    
    protected String generateTransactionReference() {
        return getProviderCode() + "-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
}