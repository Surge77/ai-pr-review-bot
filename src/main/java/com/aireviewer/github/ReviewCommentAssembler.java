package com.aireviewer.github;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aireviewer.diff.DiffLineMapper;
import com.aireviewer.model.PrReview;
import com.aireviewer.model.PrReviewComment;
import com.aireviewer.model.ReviewIssue;
import com.aireviewer.model.ReviewedFile;
import com.aireviewer.model.Severity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Turns the accumulated per-file LLM feedback into a single {@link PrReview}: a
 * Markdown summary body plus inline comments. Issues whose line is part of the
 * diff become inline comments; line-less or out-of-diff issues are folded into
 * the body so no feedback is lost (and GitHub never rejects the review).
 */
@Component
@RequiredArgsConstructor
public class ReviewCommentAssembler {

    private static final String HEADER = "## 🤖 AI Code Review";
    private static final String NO_ISSUES = HEADER + "\n\n✅ No issues found.";
    private static final Map<Severity, String> EMOJI = Map.of(
            Severity.CRITICAL, "🔴",
            Severity.WARNING, "🟡",
            Severity.SUGGESTION, "🔵");

    private final DiffLineMapper diffLineMapper;

    /**
     * Assembles a consolidated review from all reviewed files.
     *
     * @param reviews per-file feedback gathered during the pipeline
     * @return the review body and inline comments to post
     */
    public PrReview assemble(List<ReviewedFile> reviews) {
        List<PrReviewComment> inline = new ArrayList<>();
        StringBuilder general = new StringBuilder();
        int issueCount = 0;

        for (ReviewedFile review : reviews) {
            String filename = review.file().filename();
            Set<Integer> commentable = diffLineMapper.commentableLines(review.file().patch());
            for (ReviewIssue issue : review.feedback().issues()) {
                issueCount++;
                if (isInline(issue, commentable)) {
                    inline.add(new PrReviewComment(filename, issue.line(), inlineBody(issue)));
                } else {
                    general.append(generalLine(filename, issue));
                }
            }
        }

        if (issueCount == 0) {
            return new PrReview(NO_ISSUES, List.of());
        }
        return new PrReview(buildBody(reviews, general, inline.size()), inline);
    }

    private boolean isInline(ReviewIssue issue, Set<Integer> commentable) {
        return issue.line() != null && commentable.contains(issue.line());
    }

    private String inlineBody(ReviewIssue issue) {
        return emoji(issue.severity()) + " **" + issue.severity() + "** — " + issue.comment();
    }

    private String generalLine(String filename, ReviewIssue issue) {
        return "- `" + filename + "` " + emoji(issue.severity()) + " **"
                + issue.severity() + "**: " + issue.comment() + "\n";
    }

    private String buildBody(List<ReviewedFile> reviews, StringBuilder general, int inlineCount) {
        StringBuilder body = new StringBuilder(HEADER).append("\n\n");
        body.append("Reviewed ").append(reviews.size()).append(" file(s); ")
                .append(inlineCount).append(" inline comment(s).\n\n");
        for (ReviewedFile review : reviews) {
            body.append("- **").append(review.file().filename()).append("**: ")
                    .append(review.feedback().summary()).append('\n');
        }
        if (general.length() > 0) {
            body.append("\n### General notes\n").append(general);
        }
        return body.toString();
    }

    private String emoji(Severity severity) {
        return EMOJI.getOrDefault(severity, EMOJI.get(Severity.SUGGESTION));
    }
}
