package com.aireviewer.github;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.aireviewer.diff.DiffLineMapper;
import com.aireviewer.model.FileDiff;
import com.aireviewer.model.PrReview;
import com.aireviewer.model.ReviewFeedback;
import com.aireviewer.model.ReviewIssue;
import com.aireviewer.model.ReviewedFile;
import com.aireviewer.model.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewCommentAssemblerTest {

    private final ReviewCommentAssembler assembler =
            new ReviewCommentAssembler(new DiffLineMapper());

    private static FileDiff fileWithTwoNewLines(String name) {
        // new side: line 1 (+a), line 2 (+b)
        return new FileDiff(name, "modified", "@@ -0,0 +1,2 @@\n+a\n+b", "sha", 2, 0);
    }

    @Test
    @DisplayName("issue on an in-diff line becomes an inline comment")
    void in_diff_issue_is_inline() {
        ReviewIssue issue = new ReviewIssue(2, Severity.WARNING, "nullable deref");
        ReviewedFile rf = new ReviewedFile(fileWithTwoNewLines("A.java"),
                new ReviewFeedback("does X", List.of(issue), false));

        PrReview review = assembler.assemble(List.of(rf));

        assertThat(review.comments()).hasSize(1);
        assertThat(review.comments().getFirst().path()).isEqualTo("A.java");
        assertThat(review.comments().getFirst().line()).isEqualTo(2);
        assertThat(review.comments().getFirst().body()).contains("WARNING").contains("nullable deref");
    }

    @Test
    @DisplayName("line-less and out-of-diff issues fold into the body, not inline")
    void out_of_diff_issue_folds_into_body() {
        ReviewIssue lineless = new ReviewIssue(null, Severity.CRITICAL, "missing auth check");
        ReviewIssue outOfDiff = new ReviewIssue(999, Severity.SUGGESTION, "rename var");
        ReviewedFile rf = new ReviewedFile(fileWithTwoNewLines("B.java"),
                new ReviewFeedback("does Y", List.of(lineless, outOfDiff), false));

        PrReview review = assembler.assemble(List.of(rf));

        assertThat(review.comments()).isEmpty();
        assertThat(review.body()).contains("General notes")
                .contains("missing auth check").contains("rename var");
    }

    @Test
    @DisplayName("no issues across all files yields a clean approval body")
    void no_issues_clean_body() {
        ReviewedFile rf = new ReviewedFile(fileWithTwoNewLines("C.java"),
                new ReviewFeedback("does Z", List.of(), true));

        PrReview review = assembler.assemble(List.of(rf));

        assertThat(review.comments()).isEmpty();
        assertThat(review.body()).contains("No issues found");
    }

    @Test
    @DisplayName("body includes per-file summary and file count")
    void body_lists_summaries() {
        ReviewIssue issue = new ReviewIssue(1, Severity.SUGGESTION, "tidy");
        ReviewedFile rf = new ReviewedFile(fileWithTwoNewLines("D.java"),
                new ReviewFeedback("adds a guard", List.of(issue), true));

        PrReview review = assembler.assemble(List.of(rf));

        assertThat(review.body()).contains("Reviewed 1 file(s)")
                .contains("**D.java**: adds a guard");
    }
}
