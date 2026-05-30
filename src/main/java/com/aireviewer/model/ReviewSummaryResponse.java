package com.aireviewer.model;

import java.time.Instant;

/**
 * API projection of one audit row for the reviews listing. Omits the full
 * {@code llmFeedback} payload to keep list responses lean.
 *
 * @param id           audit row id
 * @param prNumber     pull request number
 * @param repoFullName {@code owner/repo}
 * @param filePath     reviewed file path ({@code *} for pipeline-level entries)
 * @param status       review outcome
 * @param issuesFound  number of issues recorded
 * @param hasCritical  whether a critical issue was found
 * @param commitSha    head commit the review ran against
 * @param createdAt    when the row was written
 */
public record ReviewSummaryResponse(
        Long id,
        int prNumber,
        String repoFullName,
        String filePath,
        ReviewStatus status,
        int issuesFound,
        boolean hasCritical,
        String commitSha,
        Instant createdAt) {

    /**
     * Maps an entity to its API projection.
     *
     * @param log the audit entity
     * @return the projection
     */
    public static ReviewSummaryResponse from(ReviewAuditLog log) {
        return new ReviewSummaryResponse(log.getId(), log.getPrNumber(), log.getRepoFullName(),
                log.getFilePath(), log.getStatus(), log.getIssuesFound(), log.isHasCritical(),
                log.getCommitSha(), log.getCreatedAt());
    }
}
