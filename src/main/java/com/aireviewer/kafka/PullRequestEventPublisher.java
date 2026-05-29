package com.aireviewer.kafka;

import com.aireviewer.model.PullRequestEvent;

/**
 * Publishes validated pull-request events onto the review pipeline. Abstracted
 * from the transport so the webhook layer can be unit-tested without Kafka.
 */
public interface PullRequestEventPublisher {

    /**
     * Publishes the event to the review-requested topic, keyed by PR so that
     * events for the same PR preserve ordering on a single partition.
     *
     * @param event the validated pull-request event to publish
     */
    void publish(PullRequestEvent event);
}
