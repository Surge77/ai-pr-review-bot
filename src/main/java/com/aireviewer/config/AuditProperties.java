package com.aireviewer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for {@code app.audit.*}.
 *
 * @param apiKey shared secret required in the {@code X-API-Key} header for
 *               {@code /api/audit/**}; requests are rejected when it is unset
 */
@ConfigurationProperties(prefix = "app.audit")
public record AuditProperties(String apiKey) {
}
