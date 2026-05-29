package com.aireviewer.audit;

import java.time.Clock;

import com.aireviewer.model.FileDiff;
import com.aireviewer.model.PullRequestEvent;
import com.aireviewer.model.ReviewAuditLog;
import com.aireviewer.model.ReviewFeedback;
import com.aireviewer.model.ReviewStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists review audit records — one per file evaluated, plus pipeline-level
 * failures.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final ReviewAuditLogRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Records a successful review of a file, including the model feedback.
     *
     * @param event    the originating PR event
     * @param file     the reviewed file
     * @param feedback the LLM feedback
     * @return the persisted record
     */
    @Transactional
    public ReviewAuditLog recordReviewed(PullRequestEvent event, FileDiff file, ReviewFeedback feedback) {
        ReviewAuditLog record = baseBuilder(event, file.filename(), ReviewStatus.REVIEWED)
                .llmFeedback(toJson(feedback))
                .issuesFound(feedback.issueCount())
                .hasCritical(feedback.hasCritical())
                .build();
        return repository.save(record);
    }

    /**
     * Records that a file was skipped because an identical diff was cached.
     *
     * @param event the originating PR event
     * @param file  the skipped file
     * @return the persisted record
     */
    @Transactional
    public ReviewAuditLog recordSkipped(PullRequestEvent event, FileDiff file) {
        return repository.save(baseBuilder(event, file.filename(), ReviewStatus.SKIPPED).build());
    }

    /**
     * Records that a single file failed to be reviewed (e.g. the LLM call failed).
     *
     * @param event the originating PR event
     * @param file  the file that failed
     * @return the persisted record
     */
    @Transactional
    public ReviewAuditLog recordFileFailure(PullRequestEvent event, FileDiff file) {
        return repository.save(baseBuilder(event, file.filename(), ReviewStatus.FAILED).build());
    }

    /**
     * Records a {@code FAILED} audit entry for an event that exhausted retries and
     * was dead-lettered. The entry is pipeline-level (no specific file).
     *
     * @param event the event that failed processing
     * @return the persisted audit record
     */
    @Transactional
    public ReviewAuditLog recordFailure(PullRequestEvent event) {
        return repository.save(
                baseBuilder(event, ReviewAuditLog.PIPELINE_LEVEL, ReviewStatus.FAILED).build());
    }

    private ReviewAuditLog.ReviewAuditLogBuilder baseBuilder(
            PullRequestEvent event, String filePath, ReviewStatus status) {
        return ReviewAuditLog.builder()
                .deliveryId(event.deliveryId())
                .prNumber(event.prNumber())
                .repoFullName(event.repoFullName())
                .filePath(filePath)
                .commitSha(event.headSha())
                .status(status)
                .issuesFound(0)
                .hasCritical(false)
                .createdAt(clock.instant());
    }

    private String toJson(ReviewFeedback feedback) {
        try {
            return objectMapper.writeValueAsString(feedback);
        } catch (Exception e) {
            log.warn("Failed to serialize review feedback: {}", e.getMessage());
            return null;
        }
    }
}
