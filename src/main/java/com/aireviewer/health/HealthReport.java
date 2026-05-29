package com.aireviewer.health;

import java.time.Instant;
import java.util.Map;

/**
 * Aggregate health snapshot returned by {@code GET /health}.
 *
 * @param status     overall status: {@code UP} only if every component is up
 * @param components per-dependency status keyed by name (db, redis, kafka)
 * @param timestamp  time the report was generated
 */
public record HealthReport(String status, Map<String, String> components, Instant timestamp) {

    public static final String UP = "UP";
    public static final String DOWN = "DOWN";
}
