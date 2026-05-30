package com.aireviewer.config;

import java.time.Duration;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Builds the GitHub {@link RestClient}: base URL, API-version and accept headers,
 * bearer authentication from the configured PAT, and bounded connect/read
 * timeouts so a stalled GitHub call can never hang a consumer thread.
 */
@Configuration
public class GitHubRestClientConfig {

    private static final String GITHUB_API_VERSION = "2022-11-28";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Creates the GitHub REST client.
     *
     * @param properties GitHub configuration (base URL + token)
     * @return a configured {@link RestClient} for GitHub API calls
     */
    @Bean
    public RestClient githubRestClient(GitHubProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
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
