package com.aireviewer.diff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DiffLineMapperTest {

    private final DiffLineMapper mapper = new DiffLineMapper();

    @Test
    @DisplayName("added and context lines are commentable; removed lines are not")
    void counts_added_and_context_lines() {
        String patch = """
                @@ -1,3 +1,4 @@
                 context-a
                -removed
                +added-b
                +added-c
                 context-d""";

        // new side: 1 context-a, 2 added-b, 3 added-c, 4 context-d
        assertThat(mapper.commentableLines(patch)).containsExactlyInAnyOrder(1, 2, 3, 4);
    }

    @Test
    @DisplayName("multiple hunks each reset the new-file counter from their header")
    void handles_multiple_hunks() {
        String patch = """
                @@ -1,1 +1,1 @@
                +first
                @@ -10,1 +20,2 @@
                 ctx
                +twenty-one""";

        assertThat(mapper.commentableLines(patch)).containsExactlyInAnyOrder(1, 20, 21);
    }

    @Test
    @DisplayName("no-newline marker line is ignored")
    void ignores_no_newline_marker() {
        String patch = """
                @@ -1 +1 @@
                +only
                \\ No newline at end of file""";

        assertThat(mapper.commentableLines(patch)).containsExactly(1);
    }

    @Test
    @DisplayName("null or blank patch yields no commentable lines")
    void blank_patch_is_empty() {
        assertThat(mapper.commentableLines(null)).isEmpty();
        assertThat(mapper.commentableLines("  ")).isEmpty();
    }

    @Test
    @DisplayName("content before the first hunk header is ignored")
    void ignores_preamble_without_hunk() {
        assertThat(mapper.commentableLines("no hunk header here\n+stray")).isEmpty();
    }
}
