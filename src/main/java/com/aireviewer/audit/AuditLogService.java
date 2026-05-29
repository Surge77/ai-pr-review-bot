package com.aireviewer.audit;

import java.time.Clock;

import com.aireviewer.model.PullRequestEvent;
import com.aireviewer.model.ReviewAuditLog;
import com.aireviewer.model.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists review audit records. Reporting queries are added in a later phase;
 * here it captures pipeline-level failures routed to the dead-letter topic.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final ReviewAuditLogRepository repository;
    private final Clock clock;

    /**
     * Records a {@code FAILED} audit entry for an event that exhausted retries and
     * was dead-lettered. The entry is pipeline-level (no specific file).
     *
     * @param event the event that failed processing
     * @return the persisted audit record
     */
    @Transactional
    public ReviewAuditLog recordFailure(PullRequestEvent event) {
        ReviewAuditLog record = ReviewAuditLog.builder()
                .deliveryId(event.deliveryId())
                .prNumber(event.prNumber())
                .repoFullName(event.repoFullName())
                .filePath(ReviewAuditLog.PIPELINE_LEVEL)
                .commitSha(event.headSha())
                .status(ReviewStatus.FAILED)
                .issuesFound(0)
                .hasCritical(false)
                .createdAt(clock.instant())
                .build();
        return repository.save(record);
    }
}
