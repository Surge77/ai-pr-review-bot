package com.aireviewer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for {@code app.webhook.rate-limit.*}: a global fixed-window cap
 * on inbound webhook deliveries, protecting the pipeline (and the LLM budget)
 * from a flood.
 *
 * @param maxRequests  max deliveries accepted per window
 * @param windowSeconds length of the window in seconds
 */
@ConfigurationProperties(prefix = "app.webhook.rate-limit")
public record WebhookRateLimitProperties(int maxRequests, int windowSeconds) {

    private static final int DEFAULT_MAX_REQUESTS = 100;
    private static final int DEFAULT_WINDOW_SECONDS = 60;

    public WebhookRateLimitProperties {
        maxRequests = maxRequests <= 0 ? DEFAULT_MAX_REQUESTS : maxRequests;
        windowSeconds = windowSeconds <= 0 ? DEFAULT_WINDOW_SECONDS : windowSeconds;
    }

    /** Window length in milliseconds. */
    public long windowMillis() {
        return windowSeconds * 1_000L;
    }
}
