package com.aireviewer.kafka;

/**
 * Central registry of Kafka topic names used by the review pipeline.
 */
public final class KafkaTopics {

    /** Inbound PR review requests published by the webhook receiver. */
    public static final String REVIEW_REQUESTED = "pr.review.requested";

    /** Dead-letter topic for events that fail processing after retries. */
    public static final String REVIEW_FAILED = "pr.review.failed";

    private KafkaTopics() {
    }
}
