package com.aireviewer.config;

import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurityHeadersFilterTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;

    private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

    @Test
    void sets_security_headers_and_continues_the_chain() throws Exception {
        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(org.mockito.ArgumentMatchers.eq("Content-Security-Policy"),
                org.mockito.ArgumentMatchers.contains("default-src 'self'"));
        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(response).setHeader("X-Frame-Options", "SAMEORIGIN");
        verify(response).setHeader("Referrer-Policy", "no-referrer");
        verify(response).setHeader(org.mockito.ArgumentMatchers.eq("Strict-Transport-Security"),
                org.mockito.ArgumentMatchers.contains("max-age"));
        verify(chain).doFilter(request, response);
    }
}
