package com.aireviewer.diff;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.aireviewer.model.DiffChunk;
import com.aireviewer.model.FileDiff;
import org.junit.jupiter.api.Test;

class DiffParserServiceTest {

    private final DiffParserService service = new DiffParserService();

    private static FileDiff file(String name, String status, String patch) {
        return new FileDiff(name, status, patch, "sha-" + name, 1, 0);
    }

    private static String repeat(char c, int n) {
        return String.valueOf(c).repeat(n);
    }

    @Test
    void small_patch_yields_a_single_chunk_with_full_content() {
        FileDiff f = file("a.java", "modified", "@@ -1 +1 @@\n-old\n+new");

        List<DiffChunk> chunks = service.toReviewableChunks(List.of(f));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().content()).isEqualTo(f.patch());
        assertThat(chunks.getFirst().index()).isZero();
        assertThat(chunks.getFirst().totalChunks()).isEqualTo(1);
        assertThat(chunks.getFirst().filename()).isEqualTo("a.java");
    }

    @Test
    void large_patch_is_split_into_overlapping_chunks() {
        // 6000 chars > MAX(3000): chunks of 2500, step 2300 -> starts 0,2300,4600 => 3 chunks
        String big = repeat('x', 6000);
        FileDiff f = file("big.java", "modified", big);

        List<DiffChunk> chunks = service.toReviewableChunks(List.of(f));

        assertThat(chunks).hasSize(3);
        assertThat(chunks).allSatisfy(c -> assertThat(c.totalChunks()).isEqualTo(3));
        assertThat(chunks.get(0).content()).hasSize(DiffParserService.CHUNK_SIZE);
        assertThat(chunks.get(1).content()).hasSize(DiffParserService.CHUNK_SIZE);
        // overlap: last 200 chars of chunk 0 equal first 200 chars of chunk 1
        String tail = chunks.get(0).content().substring(
                DiffParserService.CHUNK_SIZE - DiffParserService.CHUNK_OVERLAP);
        assertThat(chunks.get(1).content()).startsWith(tail);
        // chunks reconstruct the original when overlaps are removed
        assertThat(reassemble(chunks)).isEqualTo(big);
    }

    @Test
    void binary_and_empty_patches_are_filtered_out() {
        FileDiff binary = file("img.png", "added", null);
        FileDiff empty = file("blank.txt", "added", "   ");
        FileDiff real = file("ok.java", "modified", "@@ patch @@");

        List<DiffChunk> chunks = service.toReviewableChunks(List.of(binary, empty, real));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().filename()).isEqualTo("ok.java");
    }

    private static String reassemble(List<DiffChunk> chunks) {
        StringBuilder sb = new StringBuilder(chunks.getFirst().content());
        for (int i = 1; i < chunks.size(); i++) {
            sb.append(chunks.get(i).content().substring(DiffParserService.CHUNK_OVERLAP));
        }
        return sb.toString();
    }
}
