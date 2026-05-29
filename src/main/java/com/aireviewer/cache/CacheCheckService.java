package com.aireviewer.cache;

import java.time.Duration;

import com.aireviewer.model.FileDiff;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Decides whether a file's diff can skip LLM review because an identical diff was
 * reviewed recently, and records results after review.
 *
 * <p>The cache is an optimization, not a source of truth, so it is
 * <strong>fail-open</strong>: if Redis is unavailable, the file is reviewed
 * anyway (a warning is logged). New files ({@code status == added}) always bypass
 * the cache and are reviewed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheCheckService {

    private static final String ADDED_STATUS = "added";
    private static final String MARKER_VALUE = "1";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final CacheKeyStrategy keyStrategy;

    /**
     * Whether the file's diff has already been reviewed (cache hit).
     *
     * @param repoFullName {@code owner/repo}
     * @param file         the changed file
     * @return {@code true} only on a confirmed cache hit; {@code false} for new
     *         files, cache misses, or when Redis is unavailable (fail-open)
     */
    public boolean isAlreadyReviewed(String repoFullName, FileDiff file) {
        if (ADDED_STATUS.equalsIgnoreCase(file.status())) {
            return false;
        }
        try {
            String key = keyStrategy.key(repoFullName, file.filename(), file.patch());
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Redis unavailable during cache check for {} ({}); reviewing anyway",
                    file.filename(), e.getMessage());
            return false;
        }
    }

    /**
     * Records that a file's diff has been reviewed, with a 7-day TTL. No-op if
     * Redis is unavailable.
     *
     * @param repoFullName {@code owner/repo}
     * @param file         the reviewed file
     */
    public void markReviewed(String repoFullName, FileDiff file) {
        try {
            String key = keyStrategy.key(repoFullName, file.filename(), file.patch());
            redisTemplate.opsForValue().set(key, MARKER_VALUE, TTL);
        } catch (Exception e) {
            log.warn("Redis unavailable while caching review for {} ({})",
                    file.filename(), e.getMessage());
        }
    }
}
