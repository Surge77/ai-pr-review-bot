package com.aireviewer.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import com.aireviewer.model.AuditStatsResponse;
import com.aireviewer.model.ReviewStatus;
import com.aireviewer.model.ReviewSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    private static final String REPO = "octo/repo";

    @Mock private AuditQueryService auditQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuditController(auditQueryService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private static ReviewSummaryResponse summary() {
        return new ReviewSummaryResponse(1L, 7, REPO, "A.java", ReviewStatus.REVIEWED,
                2, true, "sha", Instant.parse("2026-05-30T00:00:00Z"));
    }

    @Test
    void reviews_returns_page_of_projections() throws Exception {
        when(auditQueryService.listReviews(eq(REPO), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(summary())));

        mockMvc.perform(get("/api/audit/reviews").param("repo", REPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].filePath").value("A.java"))
                .andExpect(jsonPath("$.content[0].status").value("REVIEWED"));
    }

    @Test
    void reviews_passes_pr_filter_through() throws Exception {
        when(auditQueryService.listReviews(eq(REPO), eq(7), any()))
                .thenReturn(new PageImpl<>(List.of(summary())));

        mockMvc.perform(get("/api/audit/reviews").param("repo", REPO).param("pr", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].prNumber").value(7));
    }

    @Test
    void stats_returns_aggregate() throws Exception {
        when(auditQueryService.stats(REPO))
                .thenReturn(AuditStatsResponse.of(REPO, 10, 5, 4, 1, 12, 3));

        mockMvc.perform(get("/api/audit/stats").param("repo", REPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.skipRate").value(0.4));
    }

    @Test
    void missing_repo_param_is_bad_request() throws Exception {
        mockMvc.perform(get("/api/audit/stats"))
                .andExpect(status().isBadRequest());
    }
}
