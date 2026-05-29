package com.aireviewer.kafka;

import com.aireviewer.model.PullRequestEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Placeholder {@link ReviewProcessor} used until the full review orchestrator is
 * wired in the orchestrator phase. That phase replaces this bean (the real
 * orchestrator is marked {@code @Primary} or this class is removed).
 */
@Slf4j
@Component
public class LoggingReviewProcessor implements ReviewProcessor {

    @Override
    public void process(PullRequestEvent event) {
        log.info("Received PR event for review repo={} pr={} action={} (pipeline not yet wired)",
                event.repoFullName(), event.prNumber(), event.action());
    }
}
