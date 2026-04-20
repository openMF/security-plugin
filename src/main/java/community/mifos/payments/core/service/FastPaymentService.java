/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.core.service;

import community.mifos.payments.core.domain.FastPayment;
import community.mifos.payments.core.domain.PaymentStatus;
import community.mifos.payments.core.domain.PaymentTransaction;
import community.mifos.payments.core.repository.PaymentTransactionRepository;
import community.mifos.payments.infrastructure.audit.AuditAction;
import community.mifos.payments.infrastructure.audit.PaymentAuditLogger;
import community.mifos.payments.infrastructure.exception.*;
import community.mifos.payments.providers.base.PaymentProvider;
import community.mifos.payments.providers.base.PaymentProviderFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Core service for orchestrating fast payment transfers.
 * Integrates with Fineract savings accounts and payment providers.
 */
@Service
public class FastPaymentService {

    private final PaymentProviderFactory providerFactory;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentAuditLogger auditLogger;

    public FastPaymentService(PaymentProviderFactory providerFactory,
                              PaymentTransactionRepository transactionRepository,
                              PaymentAuditLogger auditLogger) {
        this.providerFactory = providerFactory;
        this.transactionRepository = transactionRepository;
        this.auditLogger = auditLogger;
    }

    // -------------------------------------------------------------------------
    // Core Payment Processing
    // -------------------------------------------------------------------------

    /**
     * Processes a fast payment transfer.
     * 1. Validates request and resolves provider
     * 2. Checks idempotency
     * 3. Validates source account (Fineract)
     * 4. Calls provider to initiate payment
     * 5. Persists transaction and audit trail
     */
    @Transactional
    public PaymentTransaction processPayment(FastPayment payment) {
        // Resolve provider
        String providerCode = resolveProviderCode(payment);
        PaymentProvider provider = providerFactory.getProvider(providerCode);

        // Idempotency check
        if (payment.getIdempotencyKey() != null && !payment.getIdempotencyKey().isBlank()) {
            Optional<PaymentTransaction> existing = findByIdempotencyKey(payment.getIdempotencyKey());
            if (existing.isPresent()) {
                auditLogger.logSecurityEvent(
                    existing.get().getTransactionId(),
                    providerCode,
                    AuditAction.SECURITY_EVENT,
                    "Duplicate idempotency key detected: " + payment.getIdempotencyKey(),
                    null
                );
                throw new DuplicateTransactionException(payment.getIdempotencyKey());
            }
        }

        // Validate source account exists and has sufficient funds (Fineract integration)
        validateSourceAccount(payment);

        // Execute payment through provider template method
        PaymentTransaction transaction = provider.initiatePayment(payment);

        // Persist transaction
        transaction.setIdempotencyKey(payment.getIdempotencyKey());
        transaction.setSourceClientId(payment.getSourceClientId());
        transaction.setChannel(payment.getChannel());
        transaction.setCallbackUrl(payment.getCallbackUrl());
        
        PaymentTransaction saved = transactionRepository.save(transaction);

        // Create pending journal entry if transaction is not immediate
        if (transaction.getStatus() == PaymentStatus.PENDING || 
            transaction.getStatus() == PaymentStatus.PROCESSING) {
            reserveFunds(saved);
        } else if (transaction.getStatus() == PaymentStatus.COMPLETED) {
            postSettlementAccounting(saved);
        }

        return saved;
    }

    /**
     * Queries transaction status from provider and updates local record.
     */
    @Transactional
    public PaymentTransaction queryStatus(String transactionId, String providerCode) {
        PaymentProvider provider = providerFactory.getProvider(providerCode);
        
        PaymentTransaction localTx = transactionRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        // Query provider for latest status
        Optional<PaymentTransaction> providerTx = provider.queryTransactionStatus(transactionId);
        
        if (providerTx.isPresent()) {
            PaymentStatus oldStatus = localTx.getStatus();
            PaymentStatus newStatus = providerTx.get().getStatus();
            
            if (oldStatus != newStatus) {
                localTx.setStatus(newStatus);
                localTx.setStatusReason(providerTx.get().getStatusReason());
                
                if (newStatus == PaymentStatus.COMPLETED && localTx.getCompletedAt() == null) {
                    localTx.setCompletedAtLocal(java.time.LocalDateTime.now());
                    postSettlementAccounting(localTx);
                }
                
                transactionRepository.save(localTx);
                
                auditLogger.logStatusChange(
                    transactionId,
                    providerCode,
                    oldStatus,
                    newStatus,
                    "Provider status sync"
                );
            }
        }

        return localTx;
    }

    // -------------------------------------------------------------------------
    // Recipient Validation & Lookup
    // -------------------------------------------------------------------------

    /**
     * Validates recipient identifier format and existence with provider.
     */
    @Transactional(readOnly = true)
    public boolean validateRecipient(String identifier, String countryCode, String providerCode) {
        String resolvedProvider = providerCode != null ? providerCode : 
            providerFactory.getProviderByCountry(countryCode).getProviderCode();
        PaymentProvider provider = providerFactory.getProvider(resolvedProvider);
        return provider.validateRecipient(identifier);
    }

    /**
     * Looks up recipient display name from provider (if supported).
     */
    @Transactional(readOnly = true)
    public String lookupRecipientName(String identifier, String countryCode, String providerCode) {
        String resolvedProvider = providerCode != null ? providerCode : 
            providerFactory.getProviderByCountry(countryCode).getProviderCode();
        PaymentProvider provider = providerFactory.getProvider(resolvedProvider);
        
        // Most LATAM providers don't expose name lookup for privacy;
        // this is a placeholder for DICT (PIX) or CLABE validation services
        // that return the registered name.
        if (provider.validateRecipient(identifier)) {
            // In production, integrate with DICT name resolution or return masked name
            return null; // Provider implementations can override this
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // QR Code / Payment Token Generation
    // -------------------------------------------------------------------------

    /**
     * Generates a payment token (QR code payload) for receiving payments.
     */
    @Transactional(readOnly = true)
    public String generatePaymentToken(BigDecimal amount, String description, 
                                       String countryCode, String providerCode) {
        String resolvedProvider = providerCode != null ? providerCode : 
            providerFactory.getProviderByCountry(countryCode).getProviderCode();
        PaymentProvider provider = providerFactory.getProvider(resolvedProvider);
        return provider.generatePaymentToken(amount, description);
    }

    // -------------------------------------------------------------------------
    // Cancellation
    // -------------------------------------------------------------------------

    /**
     * Cancels a pending transaction.
     */
    @Transactional
    public boolean cancelTransaction(String transactionId, String providerCode) {
        PaymentProvider provider = providerFactory.getProvider(providerCode);
        
        PaymentTransaction transaction = transactionRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        if (!transaction.canBeCancelled()) {
            throw new InvalidPaymentRequestException(
                "status",
                "Transaction cannot be cancelled in status: " + transaction.getStatus()
            );
        }

        boolean cancelled = provider.cancelTransaction(transactionId);
        
        if (cancelled) {
            PaymentStatus oldStatus = transaction.getStatus();
            transaction.setStatus(PaymentStatus.CANCELLED);
            transaction.setStatusReason("Cancelled by user");
            transactionRepository.save(transaction);
            
            auditLogger.logStatusChange(
                transactionId,
                providerCode,
                oldStatus,
                PaymentStatus.CANCELLED,
                "User initiated cancellation"
            );
            
            releaseReservedFunds(transaction);
        }

        return cancelled;
    }

    // -------------------------------------------------------------------------
    // Transaction History
    // -------------------------------------------------------------------------

    /**
     * Lists transactions for a client.
     */
    @Transactional(readOnly = true)
    public List<PaymentTransaction> listTransactions(Long clientId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findBySourceClientIdOrderByCreatedAtDesc(clientId, pageable)
            .getContent();
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private String resolveProviderCode(FastPayment payment) {
        if (payment.hasExplicitProvider()) {
            return payment.getProviderCode().toUpperCase();
        }
        return providerFactory.getProviderByCountry(payment.getCountryCode()).getProviderCode();
    }

    private Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey) {
        // In production, add a dedicated query or use metadata JSON search
        // For now, this is a simplified placeholder
        return transactionRepository.findAll().stream()
            .filter(tx -> idempotencyKey.equals(tx.getIdempotencyKey()))
            .findFirst();
    }

    /**
     * Validates source account exists and belongs to client.
     * Placeholder for Fineract SavingsAccount integration.
     */
    private void validateSourceAccount(FastPayment payment) {
        // TODO: Integrate with Fineract SavingsAccountDomainService
        // - Check account exists and is active
        // - Verify client ownership
        // - Check sufficient balance
        // Example:
        // SavingsAccount account = savingsAccountRepository.findById(payment.getSourceAccountId())
        //     .orElseThrow(() -> new InvalidPaymentRequestException("sourceAccountId", "Account not found"));
        // if (!account.getClient().getId().equals(payment.getSourceClientId())) {
        //     throw new InvalidPaymentRequestException("sourceAccountId", "Account does not belong to client");
        // }
    }

    /**
     * Reserves funds in source account (creates hold).
     * Placeholder for Fineract accounting integration.
     */
    private void reserveFunds(PaymentTransaction transaction) {
        // TODO: Create journal entry:
        // Debit: Savings Account (Client)
        // Credit: Payment Clearing Account (Liability)
        // This ensures funds are available when provider settles
    }

    /**
     * Posts final settlement accounting.
     * Placeholder for Fineract accounting integration.
     */
    private void postSettlementAccounting(PaymentTransaction transaction) {
        // TODO: Finalize journal entry:
        // Debit: Payment Clearing Account
        // Credit: Provider Payable / Cash
        // And update savings account balance if not already done
    }

    /**
     * Releases reserved funds on cancellation/failure.
     */
    private void releaseReservedFunds(PaymentTransaction transaction) {
        // TODO: Reverse the hold journal entry
    }
}