package com.aireviewer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single changed file in a pull request, as returned by the GitHub
 * "list pull request files" endpoint. Binary files arrive with a {@code null}
 * patch.
 *
 * @param filename  path of the changed file
 * @param status    {@code added} / {@code modified} / {@code removed} / {@code renamed}
 * @param patch     unified diff for the file (null for binary files)
 * @param sha       blob SHA of the file
 * @param additions number of added lines
 * @param deletions number of deleted lines
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FileDiff(
        String filename,
        String status,
        String patch,
        String sha,
        int additions,
        int deletions) {
}
