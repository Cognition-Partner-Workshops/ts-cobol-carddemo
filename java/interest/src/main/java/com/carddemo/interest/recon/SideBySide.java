package com.carddemo.interest.recon;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.carddemo.interest.codec.CardDemoLayouts;
import com.carddemo.interest.codec.LayoutField;
import com.carddemo.interest.codec.RecordLayout;

/**
 * The side-by-side comparison of a legacy run's output directory with a target run's (SS2-SS4).
 *
 * <p>The comparison itself is the estate's comparator, tools/parity/compare.py, unchanged. This
 * class does what stands around it: {@link #prepare} lays the two directories out the way the
 * comparator reads them and applies the timestamp rule; {@link #classify} reads the comparator's
 * report and gives every difference line a class - a decision id, a gap id, or defect.
 * Directories in the recorded fixture format only (FI-2).
 */
public final class SideBySide {

    static final String TRANSACTIONS_DATASET = "AWS.M2.CARDDEMO.SYSTRAN.G0001V00";
    static final String ACCOUNTS_DATASET = "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS";
    static final String DEFECT = "defect";
    private static final List<String> TIMESTAMP_FIELDS = List.of("TRAN-ORIG-TS", "TRAN-PROC-TS");
    private static final Pattern DIFFERENCE_ROW = Pattern.compile("^\\| (.*?) \\| `(.*?)` \\| (.*?) \\| (.*?) \\|$");
    private static final Pattern MORE_ROW = Pattern.compile("^\\| … \\| \\| (\\d+) more \\| \\|$");
    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");

    private SideBySide() {
    }

    /** What {@link #prepare} did with the timestamps of the target's SYSTRAN records. */
    public record Prepared(Path legacyRoot, Path target, TimestampRule rule, long timestampsWellFormed, long timestampsMalformed) {
    }

    /** One difference line of the comparator with its class and the reason for it. */
    public record Classified(String where, String field, String legacy, String target, String classId, String why) {
    }

    /**
     * Copies the two directories under {@code work}: the legacy one as
     * {@code legacy/interest/<name>/expected} (with a manifest.json when it has none), the target
     * one as {@code target}. Under the FORMAT rule every well-formed timestamp of the target's
     * SYSTRAN records is replaced by the legacy record's, so the comparator reports a timestamp
     * only when its form is wrong; under EXACT nothing is changed.
     */
    public static Prepared prepare(Path legacy, Path target, Path work, String name, TimestampRule rule) throws IOException {
        if (!Files.isDirectory(legacy) || !Files.isDirectory(target)) {
            throw new IOException("not a directory: " + (Files.isDirectory(legacy) ? target : legacy));
        }
        deleteTree(work);
        Path legacyRoot = work.resolve("legacy");
        Path legacyCopy = legacyRoot.resolve("interest").resolve(name).resolve("expected");
        Path targetCopy = work.resolve("target");
        copyTree(legacy, legacyCopy);
        copyTree(target, targetCopy);
        Path manifest = legacyCopy.resolve("manifest.json");
        if (!Files.exists(manifest)) {
            Files.writeString(manifest, manifest(legacyCopy), StandardCharsets.ISO_8859_1);
        }
        long wellFormed = 0;
        long malformed = 0;
        Path targetTransactions = targetCopy.resolve("datasets").resolve(TRANSACTIONS_DATASET + ".txt");
        if (Files.isRegularFile(targetTransactions)) {
            RecordLayout layout = CardDemoLayouts.TRANSACTION;
            List<String> targetLines = Files.readAllLines(targetTransactions, StandardCharsets.ISO_8859_1);
            Path legacyTransactions = legacyCopy.resolve("datasets").resolve(TRANSACTIONS_DATASET + ".txt");
            List<String> legacyLines = Files.isRegularFile(legacyTransactions)
                    ? Files.readAllLines(legacyTransactions, StandardCharsets.ISO_8859_1) : List.of();
            List<String> written = new ArrayList<>();
            for (int i = 0; i < targetLines.size(); i++) {
                String line = pad(targetLines.get(i), layout.length());
                String legacyLine = i < legacyLines.size() ? pad(legacyLines.get(i), layout.length()) : null;
                for (String name2 : TIMESTAMP_FIELDS) {
                    LayoutField field = layout.field(name2);
                    if (line.length() < field.end() || !TimestampRule.wellFormed(line.substring(field.offset(), field.end()))) {
                        malformed++;
                        continue;
                    }
                    wellFormed++;
                    if (rule == TimestampRule.FORMAT && legacyLine != null
                            && TimestampRule.wellFormed(legacyLine.substring(field.offset(), field.end()))) {
                        line = line.substring(0, field.offset()) + legacyLine.substring(field.offset(), field.end()) + line.substring(field.end());
                    }
                }
                written.add(line.stripTrailing());
            }
            StringBuilder text = new StringBuilder();
            written.forEach(line -> text.append(line).append('\n'));
            Files.writeString(targetTransactions, text, StandardCharsets.ISO_8859_1);
        }
        return new Prepared(legacyRoot, targetCopy, rule, wellFormed, malformed);
    }

    private static String pad(String line, int length) {
        return line.length() >= length ? line : line + " ".repeat(length - line.length());
    }

    /** The manifest the comparator needs to find and decode the two datasets (FI-2). */
    private static String manifest(Path legacyCopy) throws IOException {
        return "{\n \"" + ACCOUNTS_DATASET + "\": {\n  \"layout\": \"" + CardDemoLayouts.ACCOUNT.copybook() + "\",\n  \"lrecl\": "
                + CardDemoLayouts.ACCOUNT.length() + ",\n  \"records\": " + records(legacyCopy, ACCOUNTS_DATASET) + "\n },\n \""
                + TRANSACTIONS_DATASET + "\": {\n  \"layout\": \"" + CardDemoLayouts.TRANSACTION.copybook() + "\",\n  \"lrecl\": "
                + CardDemoLayouts.TRANSACTION.length() + ",\n  \"records\": " + records(legacyCopy, TRANSACTIONS_DATASET) + "\n }\n}";
    }

    private static int records(Path dir, String dataset) throws IOException {
        Path file = dir.resolve("datasets").resolve(dataset + ".txt");
        return Files.isRegularFile(file) ? Files.readAllLines(file, StandardCharsets.ISO_8859_1).size() : 0;
    }

    /** Every difference line of the comparator's report (a Markdown table, tools/parity/compare.py write_report) with its class. */
    public static List<Classified> classify(String comparatorReport, TimestampRule rule) {
        List<Classified> classified = new ArrayList<>();
        for (String line : comparatorReport.split("\n")) {
            Matcher more = MORE_ROW.matcher(line.strip());
            if (more.matches()) {
                classified.add(new Classified("(not listed)", "(more)", "", more.group(1) + " more",
                        DEFECT, "the comparator lists the first 40 differences only; the rest count as defects until they are seen"));
                continue;
            }
            Matcher row = DIFFERENCE_ROW.matcher(line.strip());
            if (row.matches() && !row.group(1).equals("Where")) {
                classified.add(classify(row.group(1), row.group(2), row.group(3), row.group(4), rule));
            }
        }
        return classified;
    }

    static Classified classify(String where, String field, String legacy, String target, TimestampRule rule) {
        String classId = DEFECT;
        String why = "no decision or gap explains a difference in this field";
        if (TIMESTAMP_FIELDS.contains(field)) {
            boolean wellFormed = TimestampRule.wellFormed(legacy) && TimestampRule.wellFormed(target);
            if (!wellFormed) {
                why = "the timestamp does not have the form YYYY-MM-DD-HH.MM.SS.hh0000";
            } else if (rule == TimestampRule.EXACT) {
                classId = "INT-Q03";
                why = "both are well formed and differ in value: the runs took their clocks at different moments";
            } else {
                why = "well-formed timestamps are not compared under the format rule; this line should not exist";
            }
        } else if (field.equals("TRAN-ID")) {
            if (legacy.length() == 16 && target.length() == 16 && legacy.substring(10).equals(target.substring(10))
                    && !legacy.substring(0, 10).equals(target.substring(0, 10))) {
                classId = "INT-Q02";
                why = "same counter, different ten-character prefix: the runs had different PARM dates or id prefix rules";
            } else {
                why = "the six-digit counter differs";
            }
        } else if (field.equals("TRAN-AMT")) {
            if (isAmount(legacy) && isAmount(target)
                    && new BigDecimal(legacy).subtract(new BigDecimal(target)).abs().compareTo(ONE_CENT) == 0) {
                classId = "INT-Q01";
                why = "one cent apart: truncated on one side, rounded on the other";
            } else {
                why = "the amounts differ by more than the one cent rounding can explain";
            }
        } else if (field.equals("TRAN-DESC")) {
            if (!legacy.isEmpty() && !target.isEmpty() && (legacy.startsWith(target.stripTrailing()) || target.startsWith(legacy.stripTrailing()))) {
                classId = "G05";
                why = "same text, different bytes after it: STRING does not pad, the host keeps what working storage held";
            } else {
                why = "the description text differs";
            }
        } else if (field.startsWith("(")) {
            why = "a file, a record count or a text line differs";
        }
        return new Classified(where, field, legacy, target, classId, why);
    }

    private static boolean isAmount(String text) {
        return text.matches("-?\\d+(\\.\\d+)?");
    }

    /** The side-by-side report: the rule used for the timestamps (SS3), then every difference with its class (SS4). */
    public static String markdown(String name, String legacy, String target, TimestampRule rule, long wellFormed, long malformed,
            List<Classified> differences) {
        long defects = differences.stream().filter(difference -> difference.classId().equals(DEFECT)).count();
        StringBuilder text = new StringBuilder("# Side-by-side comparison - " + name + "\n\n");
        text.append("Legacy run: `").append(legacy).append("`. Target run: `").append(target).append("`.\n\n");
        text.append("Compared field by field with `tools/parity/compare.py` (unchanged).\n\n");
        text.append("Timestamp rule used (SS3) for TRAN-ORIG-TS and TRAN-PROC-TS: ").append(rule.description()).append(". ")
                .append(wellFormed).append(" timestamp(s) of the target well formed, ").append(malformed).append(" not.\n\n");
        text.append("Result: ").append(differences.size()).append(" difference(s): ").append(defects).append(" defect(s), ")
                .append(differences.size() - defects).append(" classified under a decision or a gap. ")
                .append("REL-04 (cycles) and REL-05 (bar for equal) are open; this report does not decide them.\n\n");
        if (!differences.isEmpty()) {
            text.append("| Where | Field | Legacy | Target | Class | Why |\n|---|---|---|---|---|---|\n");
            for (Classified difference : differences) {
                text.append("| ").append(difference.where()).append(" | `").append(difference.field()).append("` | ")
                        .append(difference.legacy()).append(" | ").append(difference.target()).append(" | **")
                        .append(difference.classId()).append("** | ").append(difference.why()).append(" |\n");
            }
        }
        return text.toString();
    }

    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> paths = Files.walk(from)) {
            for (Path path : paths.toList()) {
                Path copy = to.resolve(from.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(copy);
                } else {
                    Files.createDirectories(copy.getParent());
                    Files.copy(path, copy, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
