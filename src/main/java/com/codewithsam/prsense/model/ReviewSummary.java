package com.codewithsam.prsense.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReviewSummary {

    private final List<FileSummary> fileSummaries = new ArrayList<>();

    public void addFile(String path, List<LineComment> comments) {
        fileSummaries.add(new FileSummary(path, comments));
    }

    public long countBySeverity(String severity) {
        return fileSummaries.stream()
                .flatMap(f -> f.comments().stream())
                .filter(c -> severity.equalsIgnoreCase(c.getSeverity()))
                .count();
    }

    public long totalIssues() {
        return fileSummaries.stream()
                .mapToLong(f -> f.comments().size())
                .sum();
    }

    public String toMarkdown(int prId) {
        long high = countBySeverity("HIGH");
        long medium = countBySeverity("MEDIUM");
        long low = countBySeverity("LOW");
        long total = totalIssues();

        // Verdict: any HIGH or >2 MEDIUM → reject; zero issues → approve; otherwise just comment
        String verdict = high > 0 ? "Needs Changes" : medium > 2 ? "Needs Changes" : total == 0 ? "Approved" : "Comments";
        String verdictEmoji = switch (verdict) {
            case "Approved" -> "✅";
            case "Comments" -> "💬";
            default -> "❌";
        };

        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(verdictEmoji).append(" AI Code Review — PR #").append(prId).append("\n\n");

        sb.append("| Severity | Count |\n");
        sb.append("|---|---|\n");
        sb.append("| 🔴 HIGH | ").append(high).append(" |\n");
        sb.append("| 🟠 MEDIUM | ").append(medium).append(" |\n");
        sb.append("| 🟢 LOW | ").append(low).append(" |\n");
        sb.append("| **Total** | **").append(total).append("** |\n\n");

        if (total == 0) {
            sb.append("No issues found. Code looks good! 🎉\n");
            return sb.toString();
        }

        sb.append("### Files Reviewed\n\n");
        for (FileSummary fs : fileSummaries) {
            if (fs.comments().isEmpty()) {
                sb.append("- `").append(fs.path()).append("` — no issues\n");
            } else {
                long fHigh = fs.comments().stream().filter(c -> "HIGH".equalsIgnoreCase(c.getSeverity())).count();
                long fMed = fs.comments().stream().filter(c -> "MEDIUM".equalsIgnoreCase(c.getSeverity())).count();
                long fLow = fs.comments().stream().filter(c -> "LOW".equalsIgnoreCase(c.getSeverity())).count();
                sb.append("- `").append(fs.path()).append("` — ");
                if (fHigh > 0) sb.append("🔴 ").append(fHigh).append(" HIGH ");
                if (fMed > 0) sb.append("🟠 ").append(fMed).append(" MEDIUM ");
                if (fLow > 0) sb.append("🟢 ").append(fLow).append(" LOW");
                sb.append("\n");
            }
        }

        sb.append("\n*Reviewed by AI — always verify suggestions before merging.*");
        return sb.toString();
    }

    public record FileSummary(String path, List<LineComment> comments) {}
}
