package com.carddemo.interest.recon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The side-by-side report from a differences file as {@code make sidebyside-java} writes it. The
 * lines are written by hand: the differences the comparator shows when the target of
 * i02-tie-half-cent ran with rounding HALF_EVEN and another clock (12.36 against the recorded 12.35).
 */
class SideBySideCommandTest {

    private static final String CHECKED = "#\tdatasets=2\tfields=50\trecords=4\tsysout=1\tdb=0";
    private static final String BALANCE = "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS rec 1 (ACCT-ID=2003)\tACCT-CURR-BAL\t1247.85\t1247.86";
    private static final String AMOUNT = "AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001)\tTRAN-AMT\t12.35\t12.36";
    private static final String ORIGIN_TS = "AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001)\tTRAN-ORIG-TS\t"
            + "2022-07-05-10.00.00.000000\t2022-08-05-11.00.00.000000";
    private static final String PROCESSED_TS = ORIGIN_TS.replace("TRAN-ORIG-TS", "TRAN-PROC-TS");

    @TempDir
    Path temp;

    private Path legacy;
    private Path target;
    private Path differences;
    private Path report;

    @BeforeEach
    void twoDirectories() throws IOException {
        legacy = Files.createDirectories(temp.resolve("legacy"));
        target = Files.createDirectories(temp.resolve("target"));
        Files.writeString(legacy.resolve("manifest.json"), "{}");
        differences = temp.resolve("differences.tsv");
        report = temp.resolve("out").resolve("report.md");
    }

    private int run(String timestamps) {
        return SideBySideCommand.run(new String[] {"name=shadow", "legacy=" + legacy, "target=" + target,
                "differences=" + differences, "timestamps=" + timestamps, "report=" + report});
    }

    @Test
    void noDifference_isEqual_andTheRuleIsWrittenIntoTheReport() throws IOException {
        Files.write(differences, List.of(CHECKED));

        assertEquals(SideBySideCommand.EXIT_EQUAL, run("format"));
        String text = Files.readString(report);
        assertTrue(text.contains("## Side by side - shadow: **EQUAL**"), text);
        assertTrue(text.contains("`timestamps=format`: TRAN-ORIG-TS and TRAN-PROC-TS are compared for format only"), text);
    }

    @Test
    void intQ03_formatOnly_timestampsOfAnotherClockAloneLeaveTheRunsEqual() throws IOException {
        Files.write(differences, List.of(CHECKED, ORIGIN_TS, PROCESSED_TS));

        assertEquals(SideBySideCommand.EXIT_EQUAL, run("format"));
        String text = Files.readString(report);
        assertTrue(text.contains("| INT-Q03 | 2 |"), text);
        assertTrue(text.contains("| INT-Q03 | both values have the format; the clocks of the two runs differ | yes |"), text);
    }

    @Test
    void intQ03_exact_theSameTimestampsAreDifferences() throws IOException {
        Files.write(differences, List.of(CHECKED, ORIGIN_TS, PROCESSED_TS));

        assertEquals(SideBySideCommand.EXIT_DIFFERENT, run("exact"));
        String text = Files.readString(report);
        assertTrue(text.contains("## Side by side - shadow: **DIFFERENT**"), text);
        assertTrue(text.contains("`timestamps=exact`: TRAN-ORIG-TS and TRAN-PROC-TS are compared exactly"), text);
        assertFalse(text.contains("| yes |"), text);
    }

    @Test
    void everyDifferenceLine_carriesAClass() throws IOException {
        Files.write(differences, List.of(CHECKED, BALANCE, AMOUNT, ORIGIN_TS, PROCESSED_TS));

        assertEquals(SideBySideCommand.EXIT_DIFFERENT, run("format"));
        SideBySideReport result = SideBySideCommand.report("shadow", legacy.toString(), target.toString(), differences, TimestampRule.FORMAT);
        // one defect (the balance), one INT-Q01 (12.35 against 12.36), two INT-Q03 (accepted)
        assertEquals(List.of("defect", "INT-Q01", "INT-Q03", "INT-Q03"),
                result.differences().stream().map(DifferenceClasses.Classified::differenceClass).toList());
        assertEquals(2, result.accepted());
        String text = Files.readString(report);
        assertTrue(text.contains("| `ACCT-CURR-BAL` | 1247.85 | 1247.86 | defect |"), text);
        assertTrue(text.contains("| `TRAN-AMT` | 12.35 | 12.36 | INT-Q01 |"), text);
    }

    @Test
    void legacyDirectoryThatIsNotARecordedRun_isNotAvailable_neverEqual() throws IOException {
        Files.delete(legacy.resolve("manifest.json"));
        Files.write(differences, List.of(CHECKED));

        assertEquals(SideBySideCommand.EXIT_NOT_AVAILABLE, run("format"));
        String text = Files.readString(report);
        assertTrue(text.contains("**NOT AVAILABLE**"), text);
        assertFalse(text.contains("**EQUAL**"), text);
    }

    @Test
    void comparisonThatDidNotRunOrCheckedNothing_isNotAvailable_neverEqual() throws IOException {
        assertEquals(SideBySideCommand.EXIT_NOT_AVAILABLE, run("format"));
        assertTrue(Files.readString(report).contains("the comparator left no differences file"));

        Files.write(differences, List.of("#\tdatasets=0\tfields=0\trecords=0\tsysout=0\tdb=0"));
        assertEquals(SideBySideCommand.EXIT_NOT_AVAILABLE, run("format"));
        assertTrue(Files.readString(report).contains("the comparator checked no dataset"));

        Files.write(differences, List.of(CHECKED));
        Files.delete(target);
        assertEquals(SideBySideCommand.EXIT_NOT_AVAILABLE, run("format"));
        assertTrue(Files.readString(report).contains("the target directory does not exist"));
    }

    @Test
    void timestampRuleThatIsNotOffered_isRefused() {
        assertEquals(SideBySideCommand.EXIT_NOT_AVAILABLE, run("ignore"));
        assertFalse(Files.exists(report));
    }
}
