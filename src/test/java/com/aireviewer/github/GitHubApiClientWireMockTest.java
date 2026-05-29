package com.aireviewer.github;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.aireviewer.model.FileDiff;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GitHubApiClientWireMockTest {

    private static final Instant NOW = Instant.parse("2026-05-29T12:00:00Z");
    private static final String FILES_PATH = "/repos/octo/hello/pulls/1/files";

    private static final String FILES_JSON = """
            [
              {
                "filename": "src/Main.java",
                "status": "modified",
                "patch": "@@ -1 +1 @@\\n-old\\n+new",
                "sha": "abc123",
                "additions": 1,
                "deletions": 1,
                "blob_url": "https://github.com/octo/hello/blob/abc/src/Main.java"
              },
              {
                "filename": "logo.png",
                "status": "added",
                "sha": "def456",
                "additions": 0,
                "deletions": 0
              }
            ]
            """;

    private WireMockServer server;
    private RecordingSleeper sleeper;
    private GitHubApiClient client;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        sleeper = new RecordingSleeper();
        RestClient restClient = RestClient.builder().baseUrl(server.baseUrl()).build();
        client = new GitHubApiClient(restClient, sleeper, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void fetches_and_maps_changed_files_ignoring_unknown_fields() {
        server.stubFor(get(urlPathEqualTo(FILES_PATH)).willReturn(
                okJson(FILES_JSON).withHeader("X-RateLimit-Remaining", "5000")));

        List<FileDiff> files = client.fetchChangedFiles("octo", "hello", 1);

        assertThat(files).hasSize(2);
        FileDiff main = files.getFirst();
        assertThat(main.filename()).isEqualTo("src/Main.java");
        assertThat(main.status()).isEqualTo("modified");
        assertThat(main.patch()).contains("+new");
        assertThat(main.sha()).isEqualTo("abc123");
        assertThat(files.get(1).patch()).isNull(); // binary file, no patch
        assertThat(sleeper.calls).isZero();
    }

    @Test
    void does_not_back_off_when_quota_is_healthy() {
        server.stubFor(get(urlPathEqualTo(FILES_PATH)).willReturn(
                okJson(FILES_JSON).withHeader("X-RateLimit-Remaining", "4999")));

        client.fetchChangedFiles("octo", "hello", 1);

        assertThat(sleeper.calls).isZero();
    }

    @Test
    void backs_off_until_reset_when_quota_is_low() {
        long resetEpoch = NOW.getEpochSecond() + 2; // reset 2s in the future
        server.stubFor(get(urlPathEqualTo(FILES_PATH)).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withHeader("X-RateLimit-Remaining", "5")
                .withHeader("X-RateLimit-Reset", String.valueOf(resetEpoch))
                .withBody(FILES_JSON)));

        client.fetchChangedFiles("octo", "hello", 1);

        assertThat(sleeper.calls).isEqualTo(1);
        assertThat(sleeper.lastMillis).isEqualTo(2000L);
    }

    private static final class RecordingSleeper implements Sleeper {
        private int calls;
        private long lastMillis;

        @Override
        public void sleep(long millis) {
            calls++;
            lastMillis = millis;
        }
    }
}
