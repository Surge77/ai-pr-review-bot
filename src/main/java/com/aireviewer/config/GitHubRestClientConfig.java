package com.aireviewer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Builds the GitHub {@link RestClient}: base URL, API-version and accept headers,
 * and bearer authentication from the configured PAT.
 */
@Configuration
public class GitHubRestClientConfig {

    private static final String GITHUB_API_VERSION = "2022-11-28";

    /**
     * Creates the GitHub REST client.
     *
     * @param properties GitHub configuration (base URL + token)
     * @return a configured {@link RestClient} for GitHub API calls
     */
    @Bean
    public RestClient githubRestClient(GitHubProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.apiBaseUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", GITHUB_API_VERSION)
                .defaultHeaders(headers -> {
                    if (StringUtils.hasText(properties.token())) {
                        headers.setBearerAuth(properties.token());
                    }
                })
                .build();
    }
}
