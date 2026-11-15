package com.carddemo.interest.recon;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Command line of the side-by-side comparison ({@code make sidebyside-java}):
 * {@code name=<label> legacy=<dir> target=<dir> differences=<file> timestamps=format|exact report=<file>}.
 * The differences file is what tools/parity/compare.py found, one difference per line (where,
 * field, legacy, target, separated by tabs) after a first line {@code #} with what was checked.
 * Exit code 0 when no difference is left after the timestamp rule, 1 when one is, 2 when nothing
 * could be compared or on a usage error.
 */
public final class SideBySideCommand {

    static final int EXIT_EQUAL = 0;
    static final int EXIT_DIFFERENT = 1;
    static final int EXIT_NOT_AVAILABLE = 2;

    private SideBySideCommand() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        Map<String, String> arguments = ReconCommand.arguments(args);
        String name = arguments.get("name");
        String legacy = arguments.get("legacy");
        String target = arguments.get("target");
        String differencesFile = arguments.get("differences");
        String reportFile = arguments.get("report");
        TimestampRule timestampRule;
        try {
            timestampRule = TimestampRule.of(arguments.get("timestamps"));
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return EXIT_NOT_AVAILABLE;
        }
        if (name == null || legacy == null || target == null || differencesFile == null || reportFile == null) {
            System.err.println("usage: name=<label> legacy=<dir> target=<dir> differences=<file> timestamps=format|exact report=<file>");
            return EXIT_NOT_AVAILABLE;
        }
        SideBySideReport report = report(name, legacy, target, Path.of(differencesFile), timestampRule);
        try {
            Path file = Path.of(reportFile);
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, report.markdown(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("cannot write the report " + reportFile);
            return EXIT_NOT_AVAILABLE;
        }
        System.out.println(report.summaryLine());
        return switch (report.verdict()) {
            case EQUAL -> EXIT_EQUAL;
            case DIFFERENT -> EXIT_DIFFERENT;
            case NOT_AVAILABLE -> EXIT_NOT_AVAILABLE;
        };
    }

    static SideBySideReport report(String name, String legacy, String target, Path differencesFile, TimestampRule timestampRule) {
        if (!Files.isRegularFile(Path.of(legacy, "manifest.json"))) {
            return SideBySideReport.notAvailable(name, legacy, target, timestampRule,
                    "the legacy directory has no manifest.json: it is not a run in the recorded format (FI-2)");
        }
        if (!Files.isDirectory(Path.of(target))) {
            return SideBySideReport.notAvailable(name, legacy, target, timestampRule, "the target directory does not exist");
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(differencesFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return SideBySideReport.notAvailable(name, legacy, target, timestampRule, "the comparator left no differences file");
        }
        if (lines.isEmpty() || !lines.get(0).startsWith("#")) {
            return SideBySideReport.notAvailable(name, legacy, target, timestampRule,
                    "the differences file does not say what the comparator checked");
        }
        String compared = lines.get(0).substring(1).strip().replace('\t', ' ');
        if ((" " + compared + " ").contains(" datasets=0 ")) {
            return SideBySideReport.notAvailable(name, legacy, target, timestampRule, "the comparator checked no dataset");
        }
        DifferenceClasses classes = new DifferenceClasses(timestampRule);
        try {
            return new SideBySideReport(name, legacy, target, timestampRule, compared,
                    lines.stream().skip(1).filter(line -> !line.isEmpty()).map(Difference::ofLine).map(classes::classify).toList(),
                    java.util.Optional.empty());
        } catch (IllegalArgumentException e) {
            return SideBySideReport.notAvailable(name, legacy, target, timestampRule, "the differences file cannot be read: " + e.getMessage());
        }
    }
}
