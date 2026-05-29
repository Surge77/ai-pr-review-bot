package com.aireviewer.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import com.aireviewer.model.PullRequestEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class ReviewRequestConsumerTest {

    @Mock private ReviewProcessor processor;
    @Mock private Acknowledgment ack;

    private static PullRequestEvent event() {
        return new PullRequestEvent(7, "octo/repo", "octo", "h", "b", "url",
                "opened", "octo", "d-1", Instant.parse("2026-05-29T00:00:00Z"));
    }

    @Test
    void processes_event_then_commits_offset() {
        PullRequestEvent event = event();

        new ReviewRequestConsumer(processor).onMessage(event, ack);

        verify(processor).process(event);
        verify(ack).acknowledge();
    }

    @Test
    void does_not_commit_when_processing_throws() {
        PullRequestEvent event = event();
        doThrow(new RuntimeException("boom")).when(processor).process(event);

        ReviewRequestConsumer consumer = new ReviewRequestConsumer(processor);

        assertThatThrownBy(() -> consumer.onMessage(event, ack))
                .isInstanceOf(RuntimeException.class);
        verify(ack, never()).acknowledge();
    }
}
