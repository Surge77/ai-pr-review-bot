package com.aireviewer.kafka;

import com.aireviewer.model.PullRequestEvent;

/**
 * Entry point for processing a pull-request event once it is consumed from
 * Kafka. The full implementation (fetch diff → cache → LLM → comment → audit) is
 * wired in the orchestrator phase; the consumer depends only on this seam.
 */
public interface ReviewProcessor {

    /**
     * Processes a single pull-request event. Throwing from this method triggers
     * the consumer's retry-then-dead-letter handling.
     *
     * @param event the event to process
     */
    void process(PullRequestEvent event);
}
