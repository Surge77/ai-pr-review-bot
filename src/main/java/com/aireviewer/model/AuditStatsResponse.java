package com.aireviewer.model;

/**
 * Aggregate review statistics for one repository.
 *
 * @param repoFullName  {@code owner/repo}
 * @param total         total audit rows
 * @param reviewed      rows with status REVIEWED
 * @param skipped       rows with status SKIPPED (cache hits)
 * @param failed        rows with status FAILED
 * @param totalIssues   sum of issues found across reviewed files
 * @param criticalCount rows flagged with a critical issue
 * @param skipRate      skipped / total, 0.0 when total is 0
 */
public record AuditStatsResponse(
        String repoFullName,
        long total,
        long reviewed,
        long skipped,
        long failed,
        long totalIssues,
        long criticalCount,
        double skipRate) {

    /**
     * Builds a stats response, computing {@code skipRate} safely.
     *
     * @return the assembled stats
     */
    public static AuditStatsResponse of(String repoFullName, long total, long reviewed,
                                        long skipped, long failed, long totalIssues, long criticalCount) {
        double skipRate = total == 0 ? 0.0 : (double) skipped / total;
        return new AuditStatsResponse(repoFullName, total, reviewed, skipped, failed,
                totalIssues, criticalCount, skipRate);
    }
}
