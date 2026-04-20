/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.providers.spei;

import community.mifos.payments.core.domain.PaymentStatus;
import community.mifos.payments.core.domain.PaymentTransaction;
import community.mifos.payments.core.repository.PaymentTransactionRepository;
import community.mifos.payments.infrastructure.audit.PaymentAuditLogger;
import community.mifos.payments.providers.spei.client.dto.SpeiTransferResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.ResponseEntity;

/**
 * End-of-day reconciliation service for SPEI (Mexico) transactions.
 * Matches internal transactions against Banxico settlement files.
 * Runs automatically via scheduled job or can be triggered manually.
 * 
 * Also supports real-time queuing of transactions for immediate reconciliation tracking.
 */
@Service
public class SpeiReconciliationService {

    private static final Logger LOG = LoggerFactory.getLogger(SpeiReconciliationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SpeiConfig config;
    private final RestTemplate restTemplate;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentAuditLogger auditLogger;

    /**
     * In-memory queue for transactions pending reconciliation.
     * Stores transaction IDs that need to be verified against settlement files.
     * Cleared after each reconciliation run.
     */
    private final ConcurrentHashMap<String, LocalDateTime> reconciliationQueue = new ConcurrentHashMap<>();

    public SpeiReconciliationService(SpeiConfig config,
                                      PaymentTransactionRepository transactionRepository,
                                      PaymentAuditLogger auditLogger) {
        this.config = config;
        this.restTemplate = new RestTemplate();
        this.transactionRepository = transactionRepository;
        this.auditLogger = auditLogger;
    }

    // -------------------------------------------------------------------------
    // Real-Time Queue (called from SpeiPaymentProvider.postProcessPayment)
    // -------------------------------------------------------------------------

    /**
     * Queues a transaction for reconciliation tracking.
     * Called immediately after SPEI payment execution (postProcessPayment).
     * 
     * Since SPEI settlements are real-time, we mark the transaction as needing
     * end-of-day verification against Banxico's official settlement file.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void queueForReconciliation(PaymentTransaction transaction) {
        if (transaction == null || transaction.getTransactionId() == null) {
            LOG.warn("Cannot queue null transaction for reconciliation");
            return;
        }

        String txId = transaction.getTransactionId();
        
        // Add to in-memory queue with timestamp
        reconciliationQueue.put(txId, LocalDateTime.now());
        
        // Ensure transaction is marked as not reconciled
        if (Boolean.TRUE.equals(transaction.getReconciled())) {
            transaction.setReconciled(false);
            transaction.setReconciledAt(null);
        }
        
        // Persist the reconciled flag state
        transactionRepository.save(transaction);

        LOG.info("SPEI transaction queued for reconciliation: {} (ref: {})", 
            txId, transaction.getReferenceCode());

        auditLogger.logSecurityEvent(
            txId,
            "SPEI",
            "RECONCILIATION_QUEUED",
            "Transaction queued for end-of-day reconciliation",
            null
        );
    }

    /**
     * Checks if a transaction is currently in the reconciliation queue.
     */
    public boolean isQueuedForReconciliation(String transactionId) {
        return reconciliationQueue.containsKey(transactionId);
    }

    /**
     * Returns the number of transactions currently queued for reconciliation.
     */
    public int getQueueSize() {
        return reconciliationQueue.size();
    }

    /**
     * Clears the reconciliation queue.
     * Called after successful reconciliation run.
     */
    public void clearReconciliationQueue() {
        int size = reconciliationQueue.size();
        reconciliationQueue.clear();
        LOG.info("Cleared SPEI reconciliation queue ({} entries)", size);
    }

    // -------------------------------------------------------------------------
    // Scheduled Reconciliation
    // -------------------------------------------------------------------------

    /**
     * Daily reconciliation job triggered after SPEI closes (18:00 Mexico City time).
     * Cron: Every day at 18:30 CST/CDT (30 min after SPEI closes).
     */
    @Scheduled(cron = "0 30 18 * * *", zone = "America/Mexico_City")
    public void scheduledDailyReconciliation() {
        LOG.info("Starting scheduled SPEI daily reconciliation");
        try {
            reconcileByDate(LocalDate.now(ZoneId.of("America/Mexico_City")));
        } finally {
            // Clear queue after reconciliation regardless of outcome
            clearReconciliationQueue();
        }
    }

    /**
     * Manual reconciliation endpoint for back-office operations.
     */
    @Transactional
    public ReconciliationResult reconcileByDate(LocalDate date) {
        LOG.info("Starting SPEI reconciliation for date: {}", date.format(DATE_FORMATTER));

        // 1. Fetch pending/completed SPEI transactions from our DB
        List<PaymentTransaction> pendingTransactions = transactionRepository
            .findByProviderCodeAndStatus("SPEI", PaymentStatus.PENDING);

        List<PaymentTransaction> completedTransactions = transactionRepository
            .findByProviderCodeAndStatus("SPEI", PaymentStatus.COMPLETED);

        // 2. Also check queued transactions that might not be in standard status lists
        List<PaymentTransaction> queuedTransactions = fetchQueuedTransactions();

        // 3. Fetch Banxico settlement file for the date
        List<SpeiSettlementRecord> settlementRecords = fetchSettlementFile(date);

        // 4. Match and reconcile
        int matched = 0;
        int mismatched = 0;
        int missing = 0;
        int corrected = 0;

        // Combine all transactions to check
        List<PaymentTransaction> allTransactionsToReconcile = new ArrayList<>();
        allTransactionsToReconcile.addAll(pendingTransactions);
        allTransactionsToReconcile.addAll(completedTransactions);
        allTransactionsToReconcile.addAll(queuedTransactions);

        // Remove duplicates (same transaction might appear in multiple lists)
        List<PaymentTransaction> uniqueTransactions = allTransactionsToReconcile.stream()
            .distinct()
            .toList();

        for (PaymentTransaction tx : uniqueTransactions) {
            Optional<SpeiSettlementRecord> match = findMatchingRecord(tx, settlementRecords);

            if (match.isPresent()) {
                SpeiSettlementRecord record = match.get();

                if (amountsMatch(tx, record) && statusesMatch(tx, record)) {
                    // Transaction settled as expected
                    markAsReconciled(tx, record);
                    matched++;
                } else {
                    // Amount or status mismatch - requires investigation
                    logMismatch(tx, record);
                    mismatched++;
                }
            } else {
                // Transaction not found in settlement file
                if (tx.isExpired() || isStale(tx)) {
                    // Likely failed at provider but we weren't notified
                    PaymentStatus oldStatus = tx.getStatus();
                    tx.setStatus(PaymentStatus.FAILED);
                    tx.setStatusReason("Not found in Banxico settlement file");
                    transactionRepository.save(tx);
                    corrected++;
                    
                    auditLogger.logStatusChange(
                        tx.getTransactionId(),
                        "SPEI",
                        oldStatus,
                        PaymentStatus.FAILED,
                        "Transaction not found in settlement file after expiry"
                    );
                }
                missing++;
            }
        }

        // Verify completed transactions are in settlement file
        for (PaymentTransaction tx : completedTransactions) {
            if (!Boolean.TRUE.equals(tx.getReconciled())) {
                Optional<SpeiSettlementRecord> match = findMatchingRecord(tx, settlementRecords);
                if (match.isPresent()) {
                    markAsReconciled(tx, match.get());
                    matched++;
                } else {
                    // Completed in our system but missing from settlement - investigation needed
                    logMissingSettlement(tx);
                    missing++;
                }
            }
        }

        ReconciliationResult result = new ReconciliationResult(
            date,
            pendingTransactions.size(),
            completedTransactions.size(),
            matched,
            mismatched,
            missing,
            corrected
        );

        LOG.info("SPEI reconciliation completed: {}", result);
        return result;
    }

    // -------------------------------------------------------------------------
    // Queue Helpers
    // -------------------------------------------------------------------------

    /**
     * Fetches transactions from the reconciliation queue that exist in the database.
     */
    private List<PaymentTransaction> fetchQueuedTransactions() {
        if (reconciliationQueue.isEmpty()) {
            return List.of();
        }

        List<PaymentTransaction> queued = new ArrayList<>();
        for (String txId : reconciliationQueue.keySet()) {
            transactionRepository.findByTransactionId(txId).ifPresent(queued::add);
        }
        return queued;
    }

    /**
     * Checks if a transaction is stale (queued for too long without settlement).
     */
    private boolean isStale(PaymentTransaction tx) {
        LocalDateTime queuedAt = reconciliationQueue.get(tx.getTransactionId());
        if (queuedAt != null) {
            // If queued for more than 24 hours, consider stale
            return queuedAt.plusHours(24).isBefore(LocalDateTime.now());
        }
        // Fallback: check creation time
        return tx.isExpired();
    }

    // -------------------------------------------------------------------------
    // Settlement File Processing
    // -------------------------------------------------------------------------

    /**
     * Fetches Banxico settlement file (liquidación) for the specified date.
     * In production, this integrates with Banxico's SFTP or API for settlement files.
     */
    private List<SpeiSettlementRecord> fetchSettlementFile(LocalDate date) {
        String url = config.getBaseUrl() + "/spei/v1/liquidaciones/" + date.format(DATE_FORMATTER);

        try {
            ResponseEntity<SpeiSettlementFile> response = restTemplate.getForEntity(
                url,
                SpeiSettlementFile.class
            );

            if (response.getBody() != null && response.getBody().getRegistros() != null) {
                return response.getBody().getRegistros();
            }

            LOG.warn("Empty settlement file received from Banxico for {}", date);
            return List.of();

        } catch (Exception ex) {
            LOG.error("Failed to fetch SPEI settlement file for {}: {}", date, ex.getMessage());
            // Fallback: query individual transaction statuses
            return fetchIndividualStatuses(date);
        }
    }

    /**
     * Fallback method: queries individual transaction statuses when settlement file is unavailable.
     */
    private List<SpeiSettlementRecord> fetchIndividualStatuses(LocalDate date) {
        List<PaymentTransaction> transactions = transactionRepository
            .findByProviderCodeAndStatus("SPEI", PaymentStatus.PENDING);

        // Also include queued transactions
        transactions.addAll(fetchQueuedTransactions());

        return transactions.stream()
            .map(tx -> queryTransactionStatus(tx.getTransactionId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .distinct()
            .toList();
    }

    /**
     * Queries single transaction status from SPEI as fallback.
     */
    private Optional<SpeiSettlementRecord> queryTransactionStatus(String idOrden) {
        try {
            ResponseEntity<SpeiTransferResponse> response = restTemplate.getForEntity(
                config.getBaseUrl() + "/spei/v1/ordenes/" + idOrden,
                SpeiTransferResponse.class
            );

            SpeiTransferResponse body = response.getBody();
            if (body == null) return Optional.empty();

            return Optional.of(new SpeiSettlementRecord(
                body.getIdOrden(),
                body.getReferenciaNumerica(),
                body.getEstado(),
                body.getMonto(),
                body.getFechaLiquidacion()
            ));

        } catch (Exception ex) {
            LOG.error("Failed to query SPEI status for {}: {}", idOrden, ex.getMessage());
            return Optional.empty();
        }
    }

    // -------------------------------------------------------------------------
    // Matching Logic
    // -------------------------------------------------------------------------

    private Optional<SpeiSettlementRecord> findMatchingRecord(PaymentTransaction tx,
                                                               List<SpeiSettlementRecord> records) {
        return records.stream()
            .filter(r -> r.idOrden().equals(tx.getTransactionId()) ||
                        r.referenciaNumerica().equals(tx.getReferenceCode()))
            .findFirst();
    }

    private boolean amountsMatch(PaymentTransaction tx, SpeiSettlementRecord record) {
        if (tx.getAmount() == null || record.monto() == null) return false;

        // Allow small rounding differences (SPEI uses 2 decimals)
        BigDecimal difference = tx.getAmount().subtract(record.monto()).abs();
        return difference.compareTo(new BigDecimal("0.01")) <= 0;
    }

    private boolean statusesMatch(PaymentTransaction tx, SpeiSettlementRecord record) {
        PaymentStatus expectedStatus = mapSpeiStatus(record.estado());
        return tx.getStatus() == expectedStatus ||
               (tx.getStatus() == PaymentStatus.PENDING && expectedStatus == PaymentStatus.COMPLETED);
    }

    // -------------------------------------------------------------------------
    // Reconciliation Actions
    // -------------------------------------------------------------------------

    private void markAsReconciled(PaymentTransaction tx, SpeiSettlementRecord record) {
        tx.setReconciled(true);
        tx.setReconciledAt(java.util.Date.from(
            LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()
        ));

        // Update status if provider shows completion but we still have pending
        PaymentStatus settlementStatus = mapSpeiStatus(record.estado());
        if (tx.getStatus() == PaymentStatus.PENDING && settlementStatus == PaymentStatus.COMPLETED) {
            PaymentStatus oldStatus = tx.getStatus();
            tx.setStatus(PaymentStatus.COMPLETED);
            tx.setCompletedAtLocal(record.fechaLiquidacion() != null ?
                record.fechaLiquidacion() : LocalDateTime.now());
            
            auditLogger.logStatusChange(
                tx.getTransactionId(),
                "SPEI",
                oldStatus,
                PaymentStatus.COMPLETED,
                "Updated to COMPLETED during reconciliation"
            );
        }

        transactionRepository.save(tx);

        auditLogger.logStatusChange(
            tx.getTransactionId(),
            "SPEI",
            tx.getStatus(),
            tx.getStatus(),
            "Reconciled with Banxico settlement file"
        );

        // Remove from queue if present
        reconciliationQueue.remove(tx.getTransactionId());

        LOG.info("SPEI transaction reconciled: {} -> {}", tx.getReferenceCode(), record.estado());
    }

    private void logMismatch(PaymentTransaction tx, SpeiSettlementRecord record) {
        LOG.error("SPEI reconciliation MISMATCH: ref={} internalAmount={} settlementAmount={} " +
                  "internalStatus={} settlementStatus={}",
            tx.getReferenceCode(),
            tx.getAmount(),
            record.monto(),
            tx.getStatus(),
            record.estado()
        );

        auditLogger.logSecurityEvent(
            tx.getTransactionId(),
            "SPEI",
            "RECONCILIATION_MISMATCH",
            String.format("Amount: %s vs %s, Status: %s vs %s",
                tx.getAmount(), record.monto(),
                tx.getStatus(), record.estado()),
            null
        );
    }

    private void logMissingSettlement(PaymentTransaction tx) {
        LOG.error("SPEI transaction missing from settlement: ref={} status={}",
            tx.getReferenceCode(), tx.getStatus());

        auditLogger.logSecurityEvent(
            tx.getTransactionId(),
            "SPEI",
            "MISSING_FROM_SETTLEMENT",
            "Transaction completed internally but missing from Banxico settlement",
            null
        );
    }

    // -------------------------------------------------------------------------
    // Status Mapping
    // -------------------------------------------------------------------------

    private PaymentStatus mapSpeiStatus(String estado) {
        return switch (estado.toUpperCase()) {
            case "PENDIENTE", "REGISTRADA" -> PaymentStatus.PENDING;
            case "LIQUIDADA", "COMPLETADA" -> PaymentStatus.COMPLETED;
            case "RECHAZADA" -> PaymentStatus.FAILED;
            case "CANCELADA" -> PaymentStatus.CANCELLED;
            case "DEVUELTA" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.UNKNOWN;
        };
    }

    // -------------------------------------------------------------------------
    // Inner Records and Classes
    // -------------------------------------------------------------------------

    /**
     * Represents a single record from Banxico's settlement file.
     */
    public record SpeiSettlementRecord(
        String idOrden,
        String referenciaNumerica,
        String estado,
        BigDecimal monto,
        LocalDateTime fechaLiquidacion
    ) {}

    /**
     * Wrapper for Banxico settlement file response.
     */
    public static class SpeiSettlementFile {
        private LocalDate fechaLiquidacion;
        private List<SpeiSettlementRecord> registros;

        public LocalDate getFechaLiquidacion() { return fechaLiquidacion; }
        public void setFechaLiquidacion(LocalDate fechaLiquidacion) { this.fechaLiquidacion = fechaLiquidacion; }
        public List<SpeiSettlementRecord> getRegistros() { return registros; }
        public void setRegistros(List<SpeiSettlementRecord> registros) { this.registros = registros; }
    }

    /**
     * Result of a reconciliation run.
     */
    public record ReconciliationResult(
        LocalDate reconciliationDate,
        int pendingCount,
        int completedCount,
        int matched,
        int mismatched,
        int missing,
        int corrected
    ) {
        @Override
        public String toString() {
            return String.format(
                "ReconciliationResult[date=%s, pending=%d, completed=%d, matched=%d, " +
                "mismatched=%d, missing=%d, corrected=%d]",
                reconciliationDate, pendingCount, completedCount,
                matched, mismatched, missing, corrected
            );
        }
    }
}