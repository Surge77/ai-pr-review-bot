package com.aireviewer.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * Builds Redis cache keys for per-file review results.
 *
 * <p>Key format: {@code review:{repoFullName}:{filename}:{sha256(patch)}}. The
 * patch hash means a key only matches when the file's diff is byte-for-byte
 * identical to a previously reviewed version.
 */
@Component
public class CacheKeyStrategy {

    private static final String KEY_PREFIX = "review";

    /**
     * Builds the cache key for a file's diff.
     *
     * @param repoFullName {@code owner/repo}
     * @param filename     the file path
     * @param patch        the unified diff content
     * @return the fully-qualified Redis key
     */
    public String key(String repoFullName, String filename, String patch) {
        return KEY_PREFIX + ":" + repoFullName + ":" + filename + ":" + sha256(patch);
    }

    private String sha256(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JLS; absence is unrecoverable.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
