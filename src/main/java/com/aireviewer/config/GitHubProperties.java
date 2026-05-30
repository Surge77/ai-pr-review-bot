package com.aireviewer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for {@code app.github.*}.
 *
 * @param apiBaseUrl   GitHub REST API base URL (override for GitHub Enterprise)
 * @param token        personal access token with {@code pull_requests} read+write
 * @param postComments whether to post review comments back to the PR (dry-run when false)
 */
@ConfigurationProperties(prefix = "app.github")
public record GitHubProperties(String apiBaseUrl, String token, boolean postComments) {
}
