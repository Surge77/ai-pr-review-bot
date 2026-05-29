package com.aireviewer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for {@code app.llm.*}.
 *
 * @param timeoutSeconds maximum seconds to wait for a single LLM call
 */
@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(int timeoutSeconds) {

    public LlmProperties {
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 30;
        }
    }
}
