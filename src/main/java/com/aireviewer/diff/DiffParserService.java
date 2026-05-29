package com.aireviewer.diff;

import java.util.ArrayList;
import java.util.List;

import com.aireviewer.model.DiffChunk;
import com.aireviewer.model.FileDiff;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Turns the raw changed files of a PR into reviewable {@link DiffChunk}s: filters
 * out files with no usable patch (binary or empty), and splits oversized patches
 * into overlapping chunks so each fits comfortably in the LLM context window.
 */
@Service
public class DiffParserService {

    /** Patches longer than this (chars, ~token proxy) are split into chunks. */
    static final int MAX_PATCH_CHARS = 3000;
    /** Size of each chunk when splitting. */
    static final int CHUNK_SIZE = 2500;
    /** Overlap between consecutive chunks to preserve context across the cut. */
    static final int CHUNK_OVERLAP = 200;

    /**
     * Parses changed files into reviewable chunks, skipping non-reviewable files.
     *
     * @param files the changed files fetched from GitHub
     * @return ordered chunks across all reviewable files
     */
    public List<DiffChunk> toReviewableChunks(List<FileDiff> files) {
        List<DiffChunk> chunks = new ArrayList<>();
        for (FileDiff file : files) {
            if (!isReviewable(file)) {
                continue;
            }
            chunks.addAll(chunkFile(file));
        }
        return chunks;
    }

    /**
     * Whether a file has a patch worth reviewing (non-binary, non-empty).
     *
     * @param file the changed file
     * @return {@code true} if the file has a usable patch
     */
    public boolean isReviewable(FileDiff file) {
        return file != null && StringUtils.hasText(file.patch());
    }

    private List<DiffChunk> chunkFile(FileDiff file) {
        List<String> parts = split(file.patch());
        List<DiffChunk> result = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            result.add(new DiffChunk(file.filename(), file.status(), file.sha(),
                    i, parts.size(), parts.get(i)));
        }
        return result;
    }

    private List<String> split(String patch) {
        if (patch.length() <= MAX_PATCH_CHARS) {
            return List.of(patch);
        }
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < patch.length()) {
            int end = Math.min(start + CHUNK_SIZE, patch.length());
            parts.add(patch.substring(start, end));
            if (end >= patch.length()) {
                break;
            }
            start = end - CHUNK_OVERLAP;
        }
        return parts;
    }
}
