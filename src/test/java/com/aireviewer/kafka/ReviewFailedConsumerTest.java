package com.aireviewer.kafka;

import static org.mockito.Mockito.verify;

import java.time.Instant;

import com.aireviewer.audit.AuditLogService;
import com.aireviewer.model.PullRequestEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class ReviewFailedConsumerTest {

    @Mock private AuditLogService auditLogService;
    @Mock private Acknowledgment ack;

    @Test
    void records_failure_then_commits_offset() {
        PullRequestEvent event = new PullRequestEvent(7, "octo/repo", "octo", "h", "b", "url",
                "opened", "octo", "d-1", Instant.parse("2026-05-29T00:00:00Z"));

        new ReviewFailedConsumer(auditLogService).onFailedMessage(event, ack);

        verify(auditLogService).recordFailure(event);
        verify(ack).acknowledge();
    }
}
