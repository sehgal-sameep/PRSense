package com.codewithsam.prsense.util;

import com.codewithsam.prsense.model.FileDiff;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Builds a compact cross-file context string from all changed FileDiffs in a PR.
 * The context is injected into each individual file review so the AI can detect
 * cross-file issues: N+1 patterns, interface contract mismatches, missing
 * implementations, etc.
 */
public final class CrossFileContextBuilder {

    // Java/Kotlin: public/protected/private methods and class/interface/enum/record declarations
    private static final Pattern JAVA_METHOD = Pattern.compile(
            "^\\+\\s{1,12}(public|protected|private)(\\s+static)?(\\s+\\w+)+\\s+\\w+\\s*\\(.*\\)");
    private static final Pattern JAVA_TYPE = Pattern.compile(
            "^\\+\\s{0,4}(public|protected|private)?\\s*(class|interface|enum|record)\\s+(\\w+)");

    // TypeScript / JavaScript: function declarations, class/interface/type/export
    private static final Pattern TS_FUNCTION = Pattern.compile(
            "^\\+.*(export\\s+)?(async\\s+)?function\\s+\\w+\\s*\\(");
    private static final Pattern TS_TYPE = Pattern.compile(
            "^\\+.*(export\\s+)?(class|interface|type|enum)\\s+\\w+");
    private static final Pattern TS_ARROW = Pattern.compile(
            "^\\+\\s*(export\\s+)?(const|let)\\s+\\w+\\s*=\\s*(async\\s*)?\\(");

    // Python
    private static final Pattern PY_DEF = Pattern.compile(
            "^\\+\\s*(async\\s+)?def\\s+\\w+\\s*\\(");
    private static final Pattern PY_CLASS = Pattern.compile(
            "^\\+\\s*class\\s+\\w+");

    // Go
    private static final Pattern GO_FUNC = Pattern.compile(
            "^\\+func\\s+");

    private CrossFileContextBuilder() {}

    public static String build(List<FileDiff> diffs) {
        if (diffs == null || diffs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("## Cross-File PR Context (").append(diffs.size()).append(" files changed)\n");
        sb.append("Use this to detect cross-file issues: N+1 patterns, missing implementations, ");
        sb.append("interface contract mismatches.\n\n");

        boolean anySignatures = false;
        for (FileDiff diff : diffs) {
            List<String> signatures = extractSignatures(diff);
            if (signatures.isEmpty()) continue;

            anySignatures = true;
            sb.append("**").append(fileName(diff.getPath())).append("**")
              .append(" [").append(diff.getChangeType()).append("]\n");
            for (String sig : signatures) {
                sb.append("  → ").append(sig).append("\n");
            }
            sb.append("\n");
        }

        if (!anySignatures) return "";
        return sb.toString();
    }

    private static List<String> extractSignatures(FileDiff diff) {
        String language = LanguageUtil.detect(diff.getPath());
        String unifiedDiff = DiffUtil.buildUnifiedDiff(diff.getBefore(), diff.getAfter(), diff.getPath());
        List<String> sigs = new ArrayList<>();

        for (String line : unifiedDiff.split("\n")) {
            if (matches(line, language)) {
                String cleaned = line.substring(1).trim(); // strip leading "+"
                // Trim trailing " {" for cleaner display
                if (cleaned.endsWith("{")) cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
                sigs.add(cleaned);
            }
        }
        return sigs;
    }

    private static boolean matches(String line, String language) {
        if (!line.startsWith("+") || line.startsWith("+++")) return false;
        return switch (language) {
            case "Java", "Kotlin", "Groovy", "Scala" ->
                    JAVA_METHOD.matcher(line).find() || JAVA_TYPE.matcher(line).find();
            case "TypeScript", "TypeScript (React)", "JavaScript", "JavaScript (React)" ->
                    TS_FUNCTION.matcher(line).find() || TS_TYPE.matcher(line).find()
                            || TS_ARROW.matcher(line).find();
            case "Python" -> PY_DEF.matcher(line).find() || PY_CLASS.matcher(line).find();
            case "Go" -> GO_FUNC.matcher(line).find();
            default -> false;
        };
    }

    private static String fileName(String path) {
        if (path == null) return "unknown";
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }
}
