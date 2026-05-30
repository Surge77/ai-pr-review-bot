package com.aireviewer.audit;

import com.aireviewer.model.ReviewAuditLog;
import com.aireviewer.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for {@link ReviewAuditLog}, including the reporting
 * queries that back the audit API. All filters lead on {@code repo_full_name},
 * matching the covering indexes created in Flyway V1.
 */
public interface ReviewAuditLogRepository extends JpaRepository<ReviewAuditLog, Long> {

    Page<ReviewAuditLog> findByRepoFullNameOrderByCreatedAtDesc(String repoFullName, Pageable pageable);

    Page<ReviewAuditLog> findByRepoFullNameAndPrNumberOrderByCreatedAtDesc(
            String repoFullName, int prNumber, Pageable pageable);

    Page<ReviewAuditLog> findByRepoFullNameAndStatusOrderByCreatedAtDesc(
            String repoFullName, ReviewStatus status, Pageable pageable);

    long countByRepoFullName(String repoFullName);

    long countByRepoFullNameAndStatus(String repoFullName, ReviewStatus status);

    long countByRepoFullNameAndHasCriticalTrue(String repoFullName);

    @Query("select coalesce(sum(a.issuesFound), 0) from ReviewAuditLog a where a.repoFullName = :repo")
    long sumIssuesFound(@Param("repo") String repoFullName);
}
