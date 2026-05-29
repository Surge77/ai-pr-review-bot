package com.aireviewer.kafka;

import com.aireviewer.model.PullRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes pull-request events from {@link KafkaTopics#REVIEW_REQUESTED} and
 * hands them to the {@link ReviewProcessor}.
 *
 * <p>Offsets are committed manually only after successful processing
 * (at-least-once). An exception propagates to the container error handler, which
 * retries with exponential backoff and finally dead-letters the record.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewRequestConsumer {

    private final ReviewProcessor reviewProcessor;

    /**
     * Processes one review-requested event and commits its offset on success.
     *
     * @param event the consumed event
     * @param ack   manual acknowledgment used to commit the offset
     */
    @KafkaListener(
            topics = KafkaTopics.REVIEW_REQUESTED,
            groupId = "pr-review-group",
            containerFactory = "reviewListenerContainerFactory")
    public void onMessage(PullRequestEvent event, Acknowledgment ack) {
        log.info("Consuming PR event repo={} pr={} delivery={}",
                event.repoFullName(), event.prNumber(), event.deliveryId());
        reviewProcessor.process(event);
        ack.acknowledge();
    }
}
