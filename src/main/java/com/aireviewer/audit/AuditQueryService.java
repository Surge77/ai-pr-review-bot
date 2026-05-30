package com.aireviewer.audit;

import com.aireviewer.model.AuditStatsResponse;
import com.aireviewer.model.ReviewAuditLog;
import com.aireviewer.model.ReviewStatus;
import com.aireviewer.model.ReviewSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side of the audit store: paginated review listings and per-repo
 * aggregate statistics. Backs the {@code /api/audit} reporting endpoints.
 */
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final ReviewAuditLogRepository repository;

    /**
     * Lists audit rows for a repository, newest first, optionally narrowed to one PR.
     *
     * @param repoFullName {@code owner/repo}
     * @param prNumber     PR to filter by, or {@code null} for all PRs
     * @param pageable     page request (page, size)
     * @return a page of review projections
     */
    @Transactional(readOnly = true)
    public Page<ReviewSummaryResponse> listReviews(String repoFullName, Integer prNumber, Pageable pageable) {
        Page<ReviewAuditLog> page = prNumber == null
                ? repository.findByRepoFullNameOrderByCreatedAtDesc(repoFullName, pageable)
                : repository.findByRepoFullNameAndPrNumberOrderByCreatedAtDesc(repoFullName, prNumber, pageable);
        return page.map(ReviewSummaryResponse::from);
    }

    /**
     * Computes aggregate statistics for a repository.
     *
     * @param repoFullName {@code owner/repo}
     * @return the aggregate stats
     */
    @Transactional(readOnly = true)
    public AuditStatsResponse stats(String repoFullName) {
        long total = repository.countByRepoFullName(repoFullName);
        long reviewed = repository.countByRepoFullNameAndStatus(repoFullName, ReviewStatus.REVIEWED);
        long skipped = repository.countByRepoFullNameAndStatus(repoFullName, ReviewStatus.SKIPPED);
        long failed = repository.countByRepoFullNameAndStatus(repoFullName, ReviewStatus.FAILED);
        long totalIssues = repository.sumIssuesFound(repoFullName);
        long criticalCount = repository.countByRepoFullNameAndHasCriticalTrue(repoFullName);
        return AuditStatsResponse.of(repoFullName, total, reviewed, skipped, failed, totalIssues, criticalCount);
    }
}
