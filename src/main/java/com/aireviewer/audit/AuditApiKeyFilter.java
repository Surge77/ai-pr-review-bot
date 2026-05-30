package com.aireviewer.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.aireviewer.config.AuditProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Guards the audit reporting API: requires a valid {@code X-API-Key} header,
 * compared in constant time against the configured key. Fails closed — when no
 * key is configured, every request is rejected. Registered only for
 * {@code /api/audit/*} via {@code AuditSecurityConfig}.
 */
@RequiredArgsConstructor
public class AuditApiKeyFilter extends OncePerRequestFilter {

    static final String API_KEY_HEADER = "X-API-Key";
    private static final String UNAUTHORIZED_BODY =
            "{\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid or missing API key\"}}";

    private final AuditProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!isAuthorized(request.getHeader(API_KEY_HEADER))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "ApiKey");
            response.getWriter().write(UNAUTHORIZED_BODY);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isAuthorized(String provided) {
        String expected = properties.apiKey();
        if (!StringUtils.hasText(expected) || !StringUtils.hasText(provided)) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}
