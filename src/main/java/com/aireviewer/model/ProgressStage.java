package com.aireviewer.model;

/**
 * Lifecycle stage of a PR review, streamed to the live dashboard.
 */
public enum ProgressStage {
    STARTED,
    FILE_REVIEWED,
    FILE_SKIPPED,
    FILE_FAILED,
    COMPLETED
}
