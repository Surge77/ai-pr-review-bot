package com.aireviewer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for {@code app.github.*}.
 *
 * @param apiBaseUrl GitHub REST API base URL (override for GitHub Enterprise)
 * @param token      personal access token with {@code pull_requests} read+write
 */
@ConfigurationProperties(prefix = "app.github")
public record GitHubProperties(String apiBaseUrl, String token) {
}
