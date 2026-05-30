package com.aireviewer.model;

/**
 * Pairs a reviewed file with its LLM feedback. Accumulated across a PR so all
 * feedback can be posted as one consolidated review.
 *
 * @param file     the reviewed file
 * @param feedback the LLM feedback for that file
 */
public record ReviewedFile(FileDiff file, ReviewFeedback feedback) {
}
