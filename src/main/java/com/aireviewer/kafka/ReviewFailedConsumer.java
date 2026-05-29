package com.aireviewer.kafka;

import com.aireviewer.audit.AuditLogService;
import com.aireviewer.model.PullRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes dead-lettered events from {@link KafkaTopics#REVIEW_FAILED}: logs the
 * failure and records a {@code FAILED} audit entry so the outcome is durable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewFailedConsumer {

    private final AuditLogService auditLogService;

    /**
     * Handles a dead-lettered event by recording the failure in the audit log.
     *
     * @param event the event that exhausted retries
     * @param ack   manual acknowledgment used to commit the offset
     */
    @KafkaListener(
            topics = KafkaTopics.REVIEW_FAILED,
            groupId = "pr-review-dlt-group",
            containerFactory = "reviewListenerContainerFactory")
    public void onFailedMessage(PullRequestEvent event, Acknowledgment ack) {
        log.error("Dead-lettered PR event repo={} pr={} delivery={} - recording FAILED",
                event.repoFullName(), event.prNumber(), event.deliveryId());
        auditLogService.recordFailure(event);
        ack.acknowledge();
    }
}
