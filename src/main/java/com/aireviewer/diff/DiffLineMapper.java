package com.aireviewer.diff;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Maps a unified-diff patch to the set of file lines that can carry an inline PR
 * comment. GitHub only accepts comments on lines present in the diff's RIGHT
 * (new-file) side — added and context lines — so issues pointing elsewhere must
 * be folded into the review body instead.
 */
@Component
public class DiffLineMapper {

    private static final Pattern HUNK_HEADER =
            Pattern.compile("^@@ -\\d+(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@");

    /**
     * Computes the new-file line numbers that are part of the diff and therefore
     * valid targets for an inline comment.
     *
     * @param patch the unified diff for one file (GitHub {@code patch} field)
     * @return 1-based commentable line numbers; empty if the patch has no hunks
     */
    public Set<Integer> commentableLines(String patch) {
        Set<Integer> lines = new HashSet<>();
        if (!StringUtils.hasText(patch)) {
            return lines;
        }
        int newLine = -1;
        for (String row : patch.split("\n", -1)) {
            Matcher header = HUNK_HEADER.matcher(row);
            if (header.find()) {
                newLine = Integer.parseInt(header.group(1));
                continue;
            }
            if (newLine < 0) {
                continue;
            }
            newLine = advance(row, newLine, lines);
        }
        return lines;
    }

    private int advance(String row, int newLine, Set<Integer> lines) {
        if (row.isEmpty() || row.charAt(0) == ' ' || row.charAt(0) == '+') {
            lines.add(newLine);
            return newLine + 1;
        }
        if (row.charAt(0) == '-') {
            return newLine; // removed line: not on the new side
        }
        return newLine; // "\ No newline at end of file" and other markers
    }
}
