package community.mifos.payments.core.repository;

import community.mifos.payments.core.domain.PaymentStatus;
import community.mifos.payments.core.domain.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    Optional<PaymentTransaction> findByReferenceCode(String referenceCode);

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    List<PaymentTransaction> findBySourceAccountId(Long sourceAccountId);

    List<PaymentTransaction> findBySourceAccountIdAndStatus(Long sourceAccountId, PaymentStatus status);

    List<PaymentTransaction> findByProviderCodeAndStatus(String providerCode, PaymentStatus status);

    List<PaymentTransaction> findByProviderCodeAndStatusIn(String providerCode, List<PaymentStatus> statuses);

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.status = :status AND pt.expiresAt < :now")
    List<PaymentTransaction> findExpiredTransactions(
        @Param("status") PaymentStatus status, 
        @Param("now") Date now
    );

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.reconciled = false " +
           "AND pt.status = community.mifos.payments.core.domain.PaymentStatus.COMPLETED " +
           "AND pt.createdAt < :cutoff")
    List<PaymentTransaction> findPendingReconciliation(@Param("cutoff") Date cutoff);

    /**
     * Finds pending transactions for reconciliation by provider.
     * Used by SpeiReconciliationService and other provider reconcilers.
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.providerCode = :providerCode " +
           "AND pt.status IN ('PENDING', 'PROCESSING') " +
           "AND pt.createdAt < :cutoff")
    List<PaymentTransaction> findPendingReconciliation(
        @Param("providerCode") String providerCode,
        @Param("cutoff") Date cutoff
    );

    /**
     * Finds unreconciled completed transactions by provider.
     * Used to verify completed transactions appear in settlement files.
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.providerCode = :providerCode " +
           "AND pt.status = 'COMPLETED' " +
           "AND pt.reconciled = false " +
           "AND pt.createdAt BETWEEN :from AND :to")
    List<PaymentTransaction> findUnreconciledCompleted(
        @Param("providerCode") String providerCode,
        @Param("from") Date from,
        @Param("to") Date to
    );

    @Modifying
    @Query("UPDATE PaymentTransaction pt SET pt.status = :newStatus, pt.statusReason = :reason, " +
           "pt.updatedAt = CURRENT_TIMESTAMP WHERE pt.id = :id AND pt.version = :version")
    int updateStatusOptimistic(
        @Param("id") Long id,
        @Param("version") Long version,
        @Param("newStatus") PaymentStatus newStatus,
        @Param("reason") String reason
    );

    /**
     * Bulk update status for reconciliation marking.
     */
    @Modifying
    @Query("UPDATE PaymentTransaction pt SET pt.reconciled = true, pt.reconciledAt = :reconciledAt " +
           "WHERE pt.id = :id")
    int markReconciled(
        @Param("id") Long id,
        @Param("reconciledAt") Date reconciledAt
    );

    Page<PaymentTransaction> findBySourceClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.sourceClientId = :clientId " +
           "AND pt.createdAt BETWEEN :from AND :to ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> findByClientAndDateRange(
        @Param("clientId") Long clientId,
        @Param("from") Date from,
        @Param("to") Date to
    );

    /**
     * Finds transactions by provider and date range for reconciliation reports.
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.providerCode = :providerCode " +
           "AND pt.createdAt BETWEEN :from AND :to ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> findByProviderAndDateRange(
        @Param("providerCode") String providerCode,
        @Param("from") Date from,
        @Param("to") Date to
    );

    boolean existsByReferenceCode(String referenceCode);

    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Counts transactions by status for monitoring dashboards.
     */
    @Query("SELECT COUNT(pt) FROM PaymentTransaction pt WHERE pt.providerCode = :providerCode " +
           "AND pt.status = :status AND pt.createdAt >= :since")
    long countByProviderAndStatusSince(
        @Param("providerCode") String providerCode,
        @Param("status") PaymentStatus status,
        @Param("since") Date since
    );

    /**
     * Finds transactions that need webhook retry.
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.webhookDelivered = false " +
           "AND pt.webhookAttempts < :maxAttempts " +
           "AND pt.status IN ('COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED') " +
           "AND pt.updatedAt > :since")
    List<PaymentTransaction> findFailedWebhooks(
        @Param("maxAttempts") int maxAttempts,
        @Param("since") Date since
    );
}