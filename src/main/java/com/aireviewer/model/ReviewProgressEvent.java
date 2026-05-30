package com.aireviewer.model;

import java.time.Instant;

/**
 * A single live-progress update for a PR review, broadcast over STOMP to the
 * dashboard. Carries enough identity ({@code deliveryId}, repo, PR) for clients
 * to group updates, plus a counter pair for a progress bar.
 *
 * @param deliveryId   GitHub delivery id correlating all updates for one event
 * @param repoFullName {@code owner/repo}
 * @param prNumber     pull request number
 * @param stage        lifecycle stage this update represents
 * @param filename     file the update concerns, or {@code null} for PR-level stages
 * @param filesTotal   total changed files in the PR
 * @param filesDone    files processed so far (1..filesTotal)
 * @param message      human-readable description
 * @param timestamp    when the update was emitted
 */
public record ReviewProgressEvent(
        String deliveryId,
        String repoFullName,
        int prNumber,
        ProgressStage stage,
        String filename,
        int filesTotal,
        int filesDone,
        String message,
        Instant timestamp) {
}
