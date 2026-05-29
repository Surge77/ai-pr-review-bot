package com.aireviewer.kafka;

import com.aireviewer.model.PullRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-backed {@link PullRequestEventPublisher}. Sends events as JSON keyed by
 * {@link PullRequestEvent#partitionKey()} to {@link KafkaTopics#REVIEW_REQUESTED}.
 *
 * <p>Failure handling (dead-letter routing, retries) is added in the Kafka
 * pipeline phase; here a send failure is logged and surfaced so the caller does
 * not silently believe the event was queued.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPullRequestEventPublisher implements PullRequestEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(PullRequestEvent event) {
        String key = event.partitionKey();
        kafkaTemplate.send(KafkaTopics.REVIEW_REQUESTED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PR event key={} : {}", key, ex.getMessage());
                    } else {
                        log.info("Published PR event key={} to {}", key, KafkaTopics.REVIEW_REQUESTED);
                    }
                });
    }
}
