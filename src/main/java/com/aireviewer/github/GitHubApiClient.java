package com.aireviewer.github;

import java.time.Clock;
import java.util.List;

import com.aireviewer.model.FileDiff;
import com.aireviewer.model.PrReview;
import com.aireviewer.model.PrReviewComment;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client for the GitHub REST API. Authenticates via the configured PAT and is
 * rate-limit aware: when the remaining quota is low it backs off until the
 * reported reset time before returning, so subsequent calls are not throttled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubApiClient {

    private static final String FILES_PATH = "/repos/{owner}/{repo}/pulls/{number}/files?per_page=100";
    private static final String REVIEWS_PATH = "/repos/{owner}/{repo}/pulls/{number}/reviews";
    private static final String EVENT_COMMENT = "COMMENT";
    private static final String SIDE_RIGHT = "RIGHT";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";
    private static final int RATE_LIMIT_THRESHOLD = 10;
    private static final long DEFAULT_BACKOFF_MS = 1_000L;
    private static final long MAX_BACKOFF_MS = 60_000L;

    private final RestClient githubRestClient;
    private final Sleeper sleeper;
    private final Clock clock;

    /**
     * Fetches the changed files (with patches) for a pull request.
     *
     * @param owner      repository owner
     * @param repo       repository name
     * @param prNumber   pull request number
     * @return the changed files; empty if none
     */
    public List<FileDiff> fetchChangedFiles(String owner, String repo, int prNumber) {
        ResponseEntity<FileDiff[]> response = githubRestClient.get()
                .uri(FILES_PATH, owner, repo, prNumber)
                .retrieve()
                .toEntity(FileDiff[].class);

        backoffIfRateLimited(response.getHeaders());

        FileDiff[] body = response.getBody();
        return body == null ? List.of() : List.of(body);
    }

    /**
     * Posts a consolidated review (summary body + inline comments) to a pull
     * request. Failures are isolated — logged and swallowed — so a posting problem
     * never aborts the rest of the pipeline; the audit log remains the record of
     * what was reviewed.
     *
     * @param owner     repository owner
     * @param repo      repository name
     * @param prNumber  pull request number
     * @param commitSha head commit the review is anchored to
     * @param review    the assembled body and inline comments
     */
    public void postReview(String owner, String repo, int prNumber, String commitSha, PrReview review) {
        ReviewPayload payload = new ReviewPayload(commitSha, review.body(), EVENT_COMMENT,
                review.comments().stream().map(GitHubApiClient::toComment).toList());
        try {
            ResponseEntity<Void> response = githubRestClient.post()
                    .uri(REVIEWS_PATH, owner, repo, prNumber)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            backoffIfRateLimited(response.getHeaders());
            log.info("Posted review to {}/{}#{} ({} inline comment(s))",
                    owner, repo, prNumber, payload.comments().size());
        } catch (RestClientException e) {
            log.error("Failed to post review to {}/{}#{}: {}", owner, repo, prNumber, e.getMessage());
        }
    }

    private static CommentPayload toComment(PrReviewComment comment) {
        return new CommentPayload(comment.path(), comment.line(), SIDE_RIGHT, comment.body());
    }

    private record ReviewPayload(
            @JsonProperty("commit_id") String commitId,
            String body,
            String event,
            List<CommentPayload> comments) {
    }

    private record CommentPayload(String path, int line, String side, String body) {
    }

    private void backoffIfRateLimited(HttpHeaders headers) {
        String remaining = headers.getFirst(HEADER_REMAINING);
        if (remaining == null) {
            return;
        }
        int remainingCalls;
        try {
            remainingCalls = Integer.parseInt(remaining.trim());
        } catch (NumberFormatException e) {
            return;
        }
        if (remainingCalls >= RATE_LIMIT_THRESHOLD) {
            return;
        }
        long waitMs = waitUntilReset(headers.getFirst(HEADER_RESET));
        log.warn("GitHub rate limit low (remaining={}); backing off {} ms", remainingCalls, waitMs);
        sleeper.sleep(waitMs);
    }

    private long waitUntilReset(String resetHeader) {
        if (resetHeader == null) {
            return DEFAULT_BACKOFF_MS;
        }
        try {
            long resetEpochSeconds = Long.parseLong(resetHeader.trim());
            long deltaMs = resetEpochSeconds * 1_000L - clock.millis();
            return Math.max(0L, Math.min(deltaMs, MAX_BACKOFF_MS));
        } catch (NumberFormatException e) {
            return DEFAULT_BACKOFF_MS;
        }
    }
}
