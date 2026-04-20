/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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

    List<PaymentTransaction> findBySourceAccountId(Long sourceAccountId);

    List<PaymentTransaction> findBySourceAccountIdAndStatus(Long sourceAccountId, PaymentStatus status);

    List<PaymentTransaction> findByProviderCodeAndStatus(String providerCode, PaymentStatus status);

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.status = :status AND pt.expiresAt < :now")
    List<PaymentTransaction> findExpiredTransactions(
        @Param("status") PaymentStatus status, 
        @Param("now") Date now
    );

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.reconciled = false " +
           "AND pt.status = community.mifos.payments.core.domain.PaymentStatus.COMPLETED " +
           "AND pt.createdAt < :cutoff")
    List<PaymentTransaction> findPendingReconciliation(@Param("cutoff") Date cutoff);

    @Modifying
    @Query("UPDATE PaymentTransaction pt SET pt.status = :newStatus, pt.statusReason = :reason, " +
           "pt.updatedAt = CURRENT_TIMESTAMP WHERE pt.id = :id AND pt.version = :version")
    int updateStatusOptimistic(
        @Param("id") Long id,
        @Param("version") Long version,
        @Param("newStatus") PaymentStatus newStatus,
        @Param("reason") String reason
    );

    Page<PaymentTransaction> findBySourceClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.sourceClientId = :clientId " +
           "AND pt.createdAt BETWEEN :from AND :to ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> findByClientAndDateRange(
        @Param("clientId") Long clientId,
        @Param("from") Date from,
        @Param("to") Date to
    );

    boolean existsByReferenceCode(String referenceCode);
}