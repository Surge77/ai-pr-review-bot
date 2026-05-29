package com.aireviewer.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Structured result of an LLM code review for one file/chunk.
 *
 * @param summary  one-sentence summary of the change
 * @param issues   issues found (possibly empty)
 * @param approved whether the model approves the change
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewFeedback(String summary, List<ReviewIssue> issues, boolean approved) {

    public ReviewFeedback {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    /**
     * Fallback used when the model output cannot be parsed as valid JSON. Marks the
     * change as not approved with no issues so the pipeline can proceed safely.
     *
     * @return a non-approving fallback feedback
     */
    public static ReviewFeedback fallback() {
        return new ReviewFeedback("Automated review could not parse the model response.",
                List.of(), false);
    }

    /**
     * Number of issues found.
     *
     * @return issue count
     */
    public int issueCount() {
        return issues.size();
    }

    /**
     * Whether any issue is {@link Severity#CRITICAL}.
     *
     * @return true if a critical issue is present
     */
    public boolean hasCritical() {
        return issues.stream().anyMatch(i -> i.severity() == Severity.CRITICAL);
    }
}
