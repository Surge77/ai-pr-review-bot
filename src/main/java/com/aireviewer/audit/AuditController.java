package com.aireviewer.audit;

import com.aireviewer.model.AuditStatsResponse;
import com.aireviewer.model.ReviewStatus;
import com.aireviewer.model.ReviewSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only reporting API over the review audit log. Guarded by
 * {@code X-API-Key} (see {@code AuditApiKeyFilter}); all queries are scoped to a
 * repository.
 */
@Tag(name = "Audit", description = "Review audit log reporting")
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    /** Cap on page size so list responses stay bounded. */
    private static final int MAX_PAGE_SIZE = 100;

    private final AuditQueryService auditQueryService;

    /**
     * Lists audit rows for a repository, newest first, optionally narrowed to one PR.
     *
     * @param repo     {@code owner/repo} (required)
     * @param pr       PR number to filter by (optional)
     * @param status   review status to filter by (optional); ignored when {@code pr} is set
     * @param pageable page request; defaults to size 20, capped at {@value #MAX_PAGE_SIZE}
     * @return a page of review projections with pagination metadata
     */
    @Operation(summary = "List reviews for a repository")
    @GetMapping("/reviews")
    public PagedModel<ReviewSummaryResponse> reviews(
            @RequestParam String repo,
            @RequestParam(required = false) Integer pr,
            @RequestParam(required = false) ReviewStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return new PagedModel<>(auditQueryService.listReviews(repo, pr, status, capped(pageable)));
    }

    /**
     * Aggregate statistics for a repository.
     *
     * @param repo {@code owner/repo} (required)
     * @return the aggregate stats
     */
    @Operation(summary = "Aggregate review statistics for a repository")
    @GetMapping("/stats")
    public AuditStatsResponse stats(@RequestParam String repo) {
        return auditQueryService.stats(repo);
    }

    private Pageable capped(Pageable pageable) {
        return pageable.getPageSize() > MAX_PAGE_SIZE
                ? Pageable.ofSize(MAX_PAGE_SIZE).withPage(pageable.getPageNumber())
                : pageable;
    }
}
