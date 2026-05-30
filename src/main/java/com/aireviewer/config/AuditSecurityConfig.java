package com.aireviewer.config;

import com.aireviewer.audit.AuditApiKeyFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the {@link AuditApiKeyFilter} for the audit reporting API only, so
 * the rest of the app (webhook, health, dashboard) is unaffected.
 */
@Configuration
public class AuditSecurityConfig {

    private static final String AUDIT_PATH = "/api/audit/*";

    @Bean
    public FilterRegistrationBean<AuditApiKeyFilter> auditApiKeyFilter(AuditProperties properties) {
        FilterRegistrationBean<AuditApiKeyFilter> registration =
                new FilterRegistrationBean<>(new AuditApiKeyFilter(properties));
        registration.addUrlPatterns(AUDIT_PATH);
        return registration;
    }
}
