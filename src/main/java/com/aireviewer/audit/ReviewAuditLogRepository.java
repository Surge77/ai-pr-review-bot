package com.aireviewer.audit;

import com.aireviewer.model.ReviewAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link ReviewAuditLog}. Reporting/query methods are
 * added in the audit-reporting phase.
 */
public interface ReviewAuditLogRepository extends JpaRepository<ReviewAuditLog, Long> {
}
