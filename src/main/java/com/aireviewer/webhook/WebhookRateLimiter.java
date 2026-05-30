package com.aireviewer.webhook;

import java.time.Clock;

import com.aireviewer.config.WebhookRateLimitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Global fixed-window rate limiter for the webhook endpoint. Counts deliveries
 * within the current window and rejects once the cap is reached, until the
 * window rolls over. In-memory and single-instance — sufficient for one bot
 * process; a distributed deployment would back this with Redis.
 */
@Component
@RequiredArgsConstructor
public class WebhookRateLimiter {

    private final WebhookRateLimitProperties properties;
    private final Clock clock;

    private long windowStartMillis;
    private int count;

    /**
     * Attempts to admit one request against the current window.
     *
     * @return {@code true} if admitted, {@code false} if the window is saturated
     */
    public synchronized boolean tryAcquire() {
        long now = clock.millis();
        if (now - windowStartMillis >= properties.windowMillis()) {
            windowStartMillis = now;
            count = 1;
            return true;
        }
        if (count >= properties.maxRequests()) {
            return false;
        }
        count++;
        return true;
    }
}
