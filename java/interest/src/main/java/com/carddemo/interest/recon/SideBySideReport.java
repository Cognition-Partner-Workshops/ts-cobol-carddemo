package com.carddemo.interest.recon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.carddemo.interest.recon.DifferenceClasses.Classified;

/**
 * The side-by-side comparison of a legacy run's files with a target run's files (strategy SS2 to
 * SS4): the differences tools/parity/compare.py found, each with its class, and the timestamp rule
 * that was used. Without a comparison there is no result: NOT AVAILABLE, never equal.
 */
public record SideBySideReport(String name, String legacyDir, String targetDir, TimestampRule timestampRule,
        String compared, List<Classified> differences, Optional<String> whyNot) {

    public SideBySideReport {
        differences = List.copyOf(differences);
    }

    public static SideBySideReport notAvailable(String name, String legacyDir, String targetDir, TimestampRule timestampRule,
            String whyNot) {
        return new SideBySideReport(name, legacyDir, targetDir, timestampRule, "", List.of(), Optional.of(whyNot));
    }

    public Verdict verdict() {
        if (whyNot.isPresent()) {
            return Verdict.NOT_AVAILABLE;
        }
        return differences.stream().allMatch(Classified::isAccepted) ? Verdict.EQUAL : Verdict.DIFFERENT;
    }

    /** How many differences each class has, accepted ones included. */
    public Map<String, Long> countPerClass() {
        Map<String, Long> counts = new TreeMap<>();
        differences.forEach(difference -> counts.merge(difference.differenceClass(), 1L, Long::sum));
        return counts;
    }

    public long accepted() {
        return differences.stream().filter(Classified::isAccepted).count();
    }

    public String summaryLine() {
        return "interest/" + name + " [side by side, timestamps " + timestampRule.parameter() + "]: " + verdict().label()
                + whyNot.map(why -> " (" + why + ")").orElse(" (" + differences.size() + " difference lines, " + accepted()
                        + " accepted by the timestamp rule; per class: " + (countPerClass().isEmpty() ? "none" : countPerClass()) + ")");
    }

    public String markdown() {
        List<String> out = new ArrayList<>();
        out.add("## Side by side - " + name + ": **" + verdict().label() + "**");
        out.add("");
        out.add("Legacy run: `" + legacyDir + "`. Target run: `" + targetDir + "`. Compared field by field with "
                + "`tools/parity/compare.py`" + (compared.isEmpty() ? "" : " (" + compared + ")") + ".");
        out.add("");
        out.add("Timestamp rule (SS3), `timestamps=" + timestampRule.parameter() + "`: " + timestampRule.sentence() + ".");
        out.add("");
        if (whyNot.isPresent()) {
            out.add(Verdict.NOT_AVAILABLE.label() + ": " + whyNot.get() + ". Nothing was compared; this is not a result of equal.");
            out.add("");
            return String.join("\n", out) + "\n";
        }
        out.add("Every difference line carries a class (SS4): `defect`, the id of an open decision (INT-Q..) or the id of a "
                + "mapping gap (G..). A class other than `defect` is proposed only where the two values show it; the test lead confirms it.");
        out.add("");
        out.add("| Class | Difference lines |");
        out.add("|---|---|");
        countPerClass().forEach((differenceClass, count) -> out.add("| " + differenceClass + " | " + count + " |"));
        if (differences.isEmpty()) {
            out.add("| (none) | 0 |");
        }
        out.add("");
        if (!differences.isEmpty()) {
            out.add("| Where | Field | Legacy | Target | Class | Why | Accepted by the timestamp rule |");
            out.add("|---|---|---|---|---|---|---|");
            for (Classified classified : differences) {
                Difference difference = classified.difference();
                out.add("| " + ReconReport.cell(difference.where()) + " | `" + difference.field() + "` | "
                        + ReconReport.cell(difference.legacy()) + " | " + ReconReport.cell(difference.target()) + " | "
                        + classified.differenceClass() + " | " + ReconReport.cell(classified.why()) + " | "
                        + (classified.isAccepted() ? "yes" : "no") + " |");
            }
            out.add("");
        }
        return String.join("\n", out) + "\n";
    }
}
