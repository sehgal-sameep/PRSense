package com.codewithsam.prsense.util;

import java.util.Map;

public final class LanguageUtil {

    private static final Map<String, String> EXT_TO_LANGUAGE = Map.ofEntries(
            Map.entry(".java", "Java"),
            Map.entry(".kt", "Kotlin"),
            Map.entry(".groovy", "Groovy"),
            Map.entry(".scala", "Scala"),
            Map.entry(".ts", "TypeScript"),
            Map.entry(".tsx", "TypeScript (React)"),
            Map.entry(".js", "JavaScript"),
            Map.entry(".jsx", "JavaScript (React)"),
            Map.entry(".py", "Python"),
            Map.entry(".go", "Go"),
            Map.entry(".cs", "C#"),
            Map.entry(".cpp", "C++"),
            Map.entry(".c", "C"),
            Map.entry(".rs", "Rust"),
            Map.entry(".rb", "Ruby"),
            Map.entry(".php", "PHP"),
            Map.entry(".swift", "Swift"),
            Map.entry(".dart", "Dart"),
            Map.entry(".sh", "Shell"),
            Map.entry(".tf", "Terraform"),
            Map.entry(".html", "HTML"),
            Map.entry(".css", "CSS"),
            Map.entry(".scss", "SCSS")
    );

    private LanguageUtil() {}

    private static final Map<String, String> EXT_TO_FENCE = Map.ofEntries(
            Map.entry(".java", "java"),
            Map.entry(".kt", "kotlin"),
            Map.entry(".groovy", "groovy"),
            Map.entry(".scala", "scala"),
            Map.entry(".ts", "typescript"),
            Map.entry(".tsx", "typescript"),
            Map.entry(".js", "javascript"),
            Map.entry(".jsx", "javascript"),
            Map.entry(".py", "python"),
            Map.entry(".go", "go"),
            Map.entry(".cs", "csharp"),
            Map.entry(".cpp", "cpp"),
            Map.entry(".c", "c"),
            Map.entry(".rs", "rust"),
            Map.entry(".rb", "ruby"),
            Map.entry(".php", "php"),
            Map.entry(".swift", "swift"),
            Map.entry(".sh", "bash"),
            Map.entry(".tf", "hcl")
    );

    public static String detect(String filePath) {
        if (filePath == null) return "unknown";
        int dot = filePath.lastIndexOf('.');
        if (dot < 0) return "unknown";
        String ext = filePath.substring(dot).toLowerCase();
        return EXT_TO_LANGUAGE.getOrDefault(ext, "unknown");
    }

    /** Returns the markdown code-fence identifier for the file (e.g. "java", "typescript"). */
    public static String toCodeFence(String filePath) {
        if (filePath == null) return "";
        int dot = filePath.lastIndexOf('.');
        if (dot < 0) return "";
        String ext = filePath.substring(dot).toLowerCase();
        return EXT_TO_FENCE.getOrDefault(ext, "");
    }
}
