package com.aireviewer.model;

import java.util.List;

/**
 * An assembled pull-request review ready to post: a top-level body plus inline
 * comments anchored to specific diff lines.
 *
 * @param body     the review summary body (Markdown)
 * @param comments inline comments anchored to diff lines (possibly empty)
 */
public record PrReview(String body, List<PrReviewComment> comments) {

    public PrReview {
        comments = comments == null ? List.of() : List.copyOf(comments);
    }
}
