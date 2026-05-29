package com.aireviewer.health;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight aggregate health endpoint covering the bot's critical
 * dependencies (PostgreSQL, Redis, Kafka).
 *
 * <p>Distinct from {@code /actuator/health}: this returns a flat, dashboard-
 * friendly JSON shape and reports {@code 503 SERVICE_UNAVAILABLE} when any
 * dependency is down so external uptime checks can alert on it directly.
 */
@Tag(name = "Health", description = "Application and dependency health")
@RestController
@RequiredArgsConstructor
public class HealthCheckController {

    private final HealthProbeService probes;
    private final Clock clock;

    /**
     * Probes every backing dependency and returns an aggregate health report.
     *
     * @return {@code 200} with status {@code UP} when all components are up,
     *         otherwise {@code 503} with status {@code DOWN}
     */
    @Operation(summary = "Aggregate health of the bot and its dependencies")
    @GetMapping("/health")
    public ResponseEntity<HealthReport> health() {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("db", toStatus(probes.pingDatabase()));
        components.put("redis", toStatus(probes.pingRedis()));
        components.put("kafka", toStatus(probes.pingKafka()));

        boolean allUp = components.values().stream().allMatch(HealthReport.UP::equals);
        String overall = allUp ? HealthReport.UP : HealthReport.DOWN;
        HealthReport report = new HealthReport(overall, components, clock.instant());

        HttpStatus httpStatus = allUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(report);
    }

    private String toStatus(boolean up) {
        return up ? HealthReport.UP : HealthReport.DOWN;
    }
}
