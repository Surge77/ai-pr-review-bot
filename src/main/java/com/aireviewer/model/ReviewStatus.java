package com.aireviewer.model;

/**
 * Outcome of evaluating a single file (or the pipeline) during a review.
 */
public enum ReviewStatus {
    /** File was sent to the LLM and feedback was recorded. */
    REVIEWED,
    /** Unchanged file served from cache; no LLM call. */
    SKIPPED,
    /** LLM or pipeline error for this file/event. */
    FAILED
}
