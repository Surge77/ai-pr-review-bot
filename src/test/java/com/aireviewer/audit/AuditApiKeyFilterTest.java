package com.aireviewer.audit;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.aireviewer.config.AuditProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditApiKeyFilterTest {

    private static final String KEY = "s3cr3t-key";

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;

    private StringWriter body;

    @BeforeEach
    void setUp() throws Exception {
        body = new StringWriter();
    }

    private AuditApiKeyFilter filter(String configuredKey) {
        return new AuditApiKeyFilter(new AuditProperties(configuredKey));
    }

    @Test
    @DisplayName("valid key passes the request down the chain")
    void valid_key_proceeds() throws Exception {
        when(request.getHeader(AuditApiKeyFilter.API_KEY_HEADER)).thenReturn(KEY);

        filter(KEY).doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("wrong key is rejected with 401 and never reaches the chain")
    void wrong_key_rejected() throws Exception {
        when(request.getHeader(AuditApiKeyFilter.API_KEY_HEADER)).thenReturn("nope");
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter(KEY).doFilterInternal(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("missing key is rejected with 401")
    void missing_key_rejected() throws Exception {
        when(request.getHeader(AuditApiKeyFilter.API_KEY_HEADER)).thenReturn(null);
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter(KEY).doFilterInternal(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("fails closed when no key is configured, even with a header present")
    void unconfigured_key_fails_closed() throws Exception {
        when(request.getHeader(AuditApiKeyFilter.API_KEY_HEADER)).thenReturn(KEY);
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter("").doFilterInternal(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(request, response);
    }
}
