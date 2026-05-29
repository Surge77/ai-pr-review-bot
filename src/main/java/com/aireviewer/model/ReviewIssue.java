package com.aireviewer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single issue raised by the LLM during review.
 *
 * @param line     1-based line number the issue refers to, or {@code null} if not
 *                 tied to a specific line (posted as a general comment)
 * @param severity issue severity
 * @param comment  specific, actionable feedback
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewIssue(Integer line, Severity severity, String comment) {
}
