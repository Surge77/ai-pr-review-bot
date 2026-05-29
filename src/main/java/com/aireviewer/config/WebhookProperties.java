package com.aireviewer.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for {@code app.webhook.*}.
 *
 * @param secret          HMAC-SHA256 shared secret configured on the GitHub webhook
 * @param acceptedActions PR actions the bot acts on (e.g. {@code opened}, {@code synchronize})
 */
@ConfigurationProperties(prefix = "app.webhook")
public record WebhookProperties(String secret, List<String> acceptedActions) {

    public WebhookProperties {
        acceptedActions = acceptedActions == null ? List.of() : List.copyOf(acceptedActions);
    }
}
