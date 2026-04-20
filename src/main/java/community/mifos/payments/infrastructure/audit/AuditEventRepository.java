/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package community.mifos.payments.infrastructure.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByTransactionIdOrderByTimestampDesc(String transactionId);
    List<AuditEvent> findByProviderCodeAndTimestampBetween(String providerCode, LocalDateTime from, LocalDateTime to);
    List<AuditEvent> findByActionAndSeverity(AuditAction action, AuditSeverity severity);
}