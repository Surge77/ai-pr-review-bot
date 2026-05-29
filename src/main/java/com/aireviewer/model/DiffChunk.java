package com.aireviewer.model;

/**
 * A reviewable slice of a file's diff. Small patches yield a single chunk; large
 * patches are split into overlapping chunks so each fits the LLM context window.
 *
 * @param filename    path of the file this chunk belongs to
 * @param status      file status ({@code added} / {@code modified} / ...)
 * @param sha         blob SHA of the file
 * @param index       zero-based index of this chunk within the file
 * @param totalChunks total number of chunks the file was split into
 * @param content     the diff text for this chunk
 */
public record DiffChunk(
        String filename,
        String status,
        String sha,
        int index,
        int totalChunks,
        String content) {
}
