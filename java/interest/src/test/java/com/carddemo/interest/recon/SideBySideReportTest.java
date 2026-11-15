package com.carddemo.interest.recon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The side-by-side report: the timestamp rule is named, every difference line carries its class (SS3, SS4). */
class SideBySideReportTest {

    private static final String SYSTRAN = "AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001)";
    private static final List<ComparedDifference> DIFFERENCES = List.of(
            new ComparedDifference(SYSTRAN, "TRAN-ORIG-TS", "2022-07-05-10.00.00.000000", "2022-07-06-11.30.00.120000"),
            new ComparedDifference(SYSTRAN, "TRAN-AMT", "12.35", "12.36"),
            new ComparedDifference(SYSTRAN, "TRAN-CARD-NUM", "4000000000002001", "4000000000002009"));

    private static SideBySideReport report(TimestampRule rule, boolean compared, List<ComparedDifference> differences) {
        return SideBySideReport.of("legacy", "target", rule, compared, "2 datasets", differences, new DifferenceClassifier(rule, 2));
    }

    @Test
    void formatOnly_timestampDifferencesAloneLeaveTheRunsEqual() {
        SideBySideReport report = report(TimestampRule.FORMAT, true, DIFFERENCES.subList(0, 1));

        assertEquals("EQUAL", report.verdict());
        assertEquals(0, report.open());
        assertTrue(report.toMarkdown().contains("| decision INT-Q03 (accepted) |"), report.toMarkdown());
        assertTrue(report.toMarkdown().contains("timestamp rule (SS3): format only"), report.toMarkdown());
    }

    @Test
    void exact_theSameTimestampDifferenceIsADefect() {
        SideBySideReport report = report(TimestampRule.EXACT, true, DIFFERENCES.subList(0, 1));

        assertEquals("DIFFERENT", report.verdict());
        assertEquals(1, report.defects());
        assertTrue(report.toMarkdown().contains("timestamp rule (SS3): exact"), report.toMarkdown());
    }

    @Test
    void everyDifferenceLineCarriesItsClass() {
        SideBySideReport report = report(TimestampRule.FORMAT, true, DIFFERENCES);

        assertEquals(List.of("decision INT-Q03", "decision INT-Q01", "defect"),
                report.lines().stream().map(line -> line.differenceClass().label()).toList());
        assertEquals("DIFFERENT", report.verdict());
        assertEquals(1, report.defects());
        assertEquals(2, report.open());
        long tableLines = report.toMarkdown().lines().filter(line -> line.startsWith("| AWS.M2.CARDDEMO.SYSTRAN")).count();
        long classed = report.toMarkdown().lines().filter(line -> line.startsWith("| AWS.M2.CARDDEMO.SYSTRAN"))
                .filter(line -> line.contains("| defect |") || line.contains("| decision INT-")).count();
        assertEquals(3, tableLines);
        assertEquals(3, classed);
    }

    @Test
    void nothingComparedIsNotAvailable_neverEqual() {
        SideBySideReport report = report(TimestampRule.FORMAT, false, List.of());

        assertEquals("NOT AVAILABLE", report.verdict());
        assertTrue(report.toMarkdown().contains("**NOT AVAILABLE**"), report.toMarkdown());
    }

    @Test
    void classifyCommand_readsTheDifferencesFileAndExitsWithTheVerdict(@TempDir Path temp) throws Exception {
        Path differences = temp.resolve("differences.tsv");
        Path reportFile = temp.resolve("out").resolve("report.md");
        Files.writeString(differences, "#checked\t2 datasets, 4 records\n"
                + SYSTRAN + "\tTRAN-PROC-TS\t2022-07-05-10.00.00.000000\t2022-07-06-11.30.00.120000\n"
                + "SYSOUT INTCALC.STEP15\t(text)\t\t-one\\n+two\n", StandardCharsets.ISO_8859_1);
        String[] common = {"legacy=" + temp, "target=" + temp, "differences=" + differences, "reportFile=" + reportFile};

        assertEquals(ReconCommand.EXIT_DIFFERENT, ReconCommand.run(new String[] {"classify", "ts=format", common[0], common[1], common[2], common[3]}));
        String markdown = Files.readString(reportFile);
        assertTrue(markdown.contains("| decision INT-Q03 (accepted) |"), markdown);
        assertTrue(markdown.contains("| -one<br>+two | defect |"), markdown);

        Files.writeString(differences, "#checked\t2 datasets, 4 records\n", StandardCharsets.ISO_8859_1);
        assertEquals(ReconCommand.EXIT_EQUAL, ReconCommand.run(new String[] {"classify", "ts=exact", common[0], common[1], common[2], common[3]}));

        Files.writeString(differences, "#nothing\n", StandardCharsets.ISO_8859_1);
        assertEquals(ReconCommand.EXIT_DIFFERENT, ReconCommand.run(new String[] {"classify", "ts=exact", common[0], common[1], common[2], common[3]}));
        assertTrue(Files.readString(reportFile).contains("NOT AVAILABLE"));
    }

    @Test
    void unescape() {
        assertEquals("a\tb\nc\\d", ReconCommand.unescape("a\\tb\\nc\\\\d"));
    }
}
