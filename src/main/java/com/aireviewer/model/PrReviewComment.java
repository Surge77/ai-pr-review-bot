package com.aireviewer.model;

/**
 * A single inline review comment to post on a pull request.
 *
 * @param path file path the comment applies to
 * @param line 1-based line number in the file's new version (RIGHT side of the diff)
 * @param body comment text
 */
public record PrReviewComment(String path, int line, String body) {
}
