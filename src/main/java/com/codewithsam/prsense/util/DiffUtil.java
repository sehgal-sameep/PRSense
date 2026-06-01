package com.codewithsam.prsense.util;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

import java.util.Arrays;
import java.util.List;

public final class DiffUtil {

    private static final int CONTEXT_LINES = 4;

    private DiffUtil() {}

    /**
     * Generates a unified-style diff string from two file contents.
     * Returns "(new file)" or "(deleted file)" for add/delete cases.
     * Returns "(binary or empty)" when both are blank.
     */
    public static String buildUnifiedDiff(String before, String after, String filePath) {
        boolean beforeEmpty = before == null || before.isBlank();
        boolean afterEmpty = after == null || after.isBlank();

        if (beforeEmpty && afterEmpty) return "(binary or empty file — nothing to diff)";
        if (beforeEmpty) return buildNewFileDiff(after, filePath);
        if (afterEmpty) return buildDeletedFileDiff(before, filePath);

        List<String> beforeLines = Arrays.asList(before.split("\n", -1));
        List<String> afterLines = Arrays.asList(after.split("\n", -1));

        Patch<String> patch = DiffUtils.diff(beforeLines, afterLines);

        if (patch.getDeltas().isEmpty()) return "(no changes detected)";

        StringBuilder sb = new StringBuilder();
        sb.append("--- a/").append(filePath).append("\n");
        sb.append("+++ b/").append(filePath).append("\n");

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            int origPos = delta.getSource().getPosition();
            int origSize = delta.getSource().size();
            int revPos = delta.getTarget().getPosition();
            int revSize = delta.getTarget().size();

            // Hunk header
            sb.append(String.format("@@ -%d,%d +%d,%d @@%n",
                    origPos + 1, origSize, revPos + 1, revSize));

            // Context before
            int contextStart = Math.max(0, origPos - CONTEXT_LINES);
            for (int i = contextStart; i < origPos; i++) {
                sb.append(" ").append(beforeLines.get(i)).append("\n");
            }

            // Removed lines
            delta.getSource().getLines().forEach(line -> sb.append("-").append(line).append("\n"));

            // Added lines
            delta.getTarget().getLines().forEach(line -> sb.append("+").append(line).append("\n"));

            // Context after
            int contextEnd = Math.min(beforeLines.size(), origPos + origSize + CONTEXT_LINES);
            for (int i = origPos + origSize; i < contextEnd; i++) {
                sb.append(" ").append(beforeLines.get(i)).append("\n");
            }
        }

        return sb.toString();
    }

    private static String buildNewFileDiff(String after, String filePath) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- /dev/null\n");
        sb.append("+++ b/").append(filePath).append("\n");
        String[] lines = after.split("\n", -1);
        sb.append(String.format("@@ -0,0 +1,%d @@%n", lines.length));
        for (String line : lines) {
            sb.append("+").append(line).append("\n");
        }
        return sb.toString();
    }

    private static String buildDeletedFileDiff(String before, String filePath) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- a/").append(filePath).append("\n");
        sb.append("+++ /dev/null\n");
        String[] lines = before.split("\n", -1);
        sb.append(String.format("@@ -1,%d +0,0 @@%n", lines.length));
        for (String line : lines) {
            sb.append("-").append(line).append("\n");
        }
        return sb.toString();
    }
}
