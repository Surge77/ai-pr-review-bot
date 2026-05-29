package com.aireviewer.model;

import java.time.Instant;

/**
 * Internal representation of a GitHub pull-request event, published to Kafka and
 * consumed by the review pipeline. Decoupled from GitHub's wire format so the
 * rest of the system never depends on the raw webhook JSON shape.
 *
 * @param prNumber     pull request number
 * @param repoFullName {@code owner/repo}
 * @param repoOwner    repository owner login
 * @param headSha      head commit SHA the review runs against
 * @param baseSha      base commit SHA
 * @param diffUrl      URL of the PR diff
 * @param action       triggering action ({@code opened} / {@code synchronize})
 * @param senderLogin  login of the user who triggered the event
 * @param deliveryId   GitHub {@code X-GitHub-Delivery} id (idempotency key)
 * @param receivedAt   time the webhook was received
 */
public record PullRequestEvent(
        int prNumber,
        String repoFullName,
        String repoOwner,
        String headSha,
        String baseSha,
        String diffUrl,
        String action,
        String senderLogin,
        String deliveryId,
        Instant receivedAt) {

    /**
     * Kafka partition key: groups all events for a given PR onto one partition so
     * they are processed in order.
     *
     * @return {@code repoFullName#prNumber}
     */
    public String partitionKey() {
        return repoFullName + "#" + prNumber;
    }
}
