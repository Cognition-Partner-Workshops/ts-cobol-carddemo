package com.carddemo.interest.recon;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The side-by-side comparison of a legacy run's files with a target run's files (strategy items
 * SS2-SS4). The differences are those of tools/parity/compare.py, field by field; this report adds
 * the class of every one and names the timestamp rule that was used. It does not decide the exit
 * of the period (REL-04, REL-05 are open): it counts.
 */
public record SideBySideReport(String legacyDir, String targetDir, TimestampRule timestampRule, boolean compared,
        String checked, List<Line> lines) {

    /** One difference line with its class. */
    public record Line(ComparedDifference difference, DifferenceClass differenceClass) {
    }

    public static SideBySideReport of(String legacyDir, String targetDir, TimestampRule rule, boolean compared, String checked,
            List<ComparedDifference> differences, DifferenceClassifier classifier) {
        List<Line> lines = new ArrayList<>();
        differences.forEach(difference -> lines.add(new Line(difference, classifier.classify(difference))));
        return new SideBySideReport(legacyDir, targetDir, rule, compared, checked, lines);
    }

    public long defects() {
        return lines.stream().filter(line -> line.differenceClass().kind() == DifferenceClass.Kind.DEFECT).count();
    }

    /** Differences that are not accepted by the comparison rule: defects, and open decisions and gaps. */
    public long open() {
        return lines.stream().filter(line -> !line.differenceClass().accepted()).count();
    }

    /** Nothing was compared: that is NOT AVAILABLE, not equal. Otherwise equal means no open difference. */
    public String verdict() {
        if (!compared) {
            return ReconStatus.NOT_AVAILABLE.label();
        }
        return open() == 0 ? ReconStatus.EQUAL.label() : ReconStatus.DIFFERENT.label();
    }

    public String toMarkdown() {
        StringBuilder out = new StringBuilder("# Side-by-side comparison\n\n");
        out.append("- legacy run: ").append(legacyDir).append('\n');
        out.append("- target run: ").append(targetDir).append('\n');
        out.append("- timestamp rule (SS3): ").append(timestampRule.description()).append('\n');
        out.append("- compared: ").append(compared ? checked : "nothing - the legacy directory has no manifest.json naming its datasets").append('\n');
        out.append("- result: **").append(verdict()).append("** - ").append(lines.size()).append(" differences, ")
                .append(defects()).append(" defects, ").append(open() - defects()).append(" open decisions or gaps, ")
                .append(lines.size() - open()).append(" accepted by the timestamp rule\n\n");
        Map<String, Long> perClass = new LinkedHashMap<>();
        lines.forEach(line -> perClass.merge(line.differenceClass().label(), 1L, Long::sum));
        if (!perClass.isEmpty()) {
            out.append("| Class | Differences |\n|---|---|\n");
            perClass.forEach((label, count) -> out.append("| ").append(label).append(" | ").append(count).append(" |\n"));
            out.append("\n| Where | Field | Legacy | Target | Class | Why |\n|---|---|---|---|---|---|\n");
            for (Line line : lines) {
                ComparedDifference difference = line.difference();
                out.append("| ").append(cell(difference.where())).append(" | `").append(cell(difference.field())).append("` | ")
                        .append(cell(difference.legacy())).append(" | ").append(cell(difference.target())).append(" | ")
                        .append(line.differenceClass().label()).append(line.differenceClass().accepted() ? " (accepted)" : "")
                        .append(" | ").append(line.differenceClass().reason()).append(" |\n");
            }
        }
        out.append("\nThe exit of the side-by-side period (REL-04: cycles without a defect or an unclassified difference;"
                + " REL-05: the bar for equal) is not decided by this report.\n");
        return out.toString();
    }

    private static String cell(String text) {
        return text.replace("|", "\\|").replace("\n", "<br>");
    }
}
