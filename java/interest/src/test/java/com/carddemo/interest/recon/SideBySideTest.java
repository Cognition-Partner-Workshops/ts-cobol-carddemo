package com.carddemo.interest.recon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.carddemo.interest.Fixtures;

/** INT-T10, side by side: the timestamp rule of SS3 and the class of every difference (SS4). */
class SideBySideTest {

    private static final String BASELINE = "i01-baseline";
    private static final String SYSTRAN = "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt";
    private static final String LEGACY_TS = "2022-07-05-10.00.00.000000";
    private static final String LATER_TS = "2022-07-06-01.02.03.450000";

    @TempDir
    Path temp;

    private Path copyOfExpected(String name) throws IOException {
        Path from = Fixtures.expected(BASELINE);
        Path to = temp.resolve(name);
        try (Stream<Path> paths = Files.walk(from)) {
            for (Path path : paths.toList()) {
                Path copy = to.resolve(from.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(copy);
                } else {
                    Files.copy(path, copy);
                }
            }
        }
        return to;
    }

    private static void replaceInTransactions(Path dir, String from, String to) throws IOException {
        Path file = dir.resolve(SYSTRAN);
        String text = Files.readString(file, StandardCharsets.ISO_8859_1);
        assertTrue(text.contains(from));
        Files.writeString(file, text.replace(from, to), StandardCharsets.ISO_8859_1);
    }

    @Test
    void formatRule_wellFormedTimestampsOfAnotherMoment_areNotADifference() throws IOException {
        Path target = copyOfExpected("target");
        replaceInTransactions(target, LEGACY_TS, LATER_TS);

        SideBySide.Prepared prepared = SideBySide.prepare(Fixtures.expected(BASELINE), target, temp.resolve("work"), BASELINE, TimestampRule.FORMAT);

        assertEquals(4, prepared.timestampsWellFormed());
        assertEquals(0, prepared.timestampsMalformed());
        assertEquals(Fixtures.text(Fixtures.expected(BASELINE).resolve(SYSTRAN)), Fixtures.text(prepared.target().resolve(SYSTRAN)));
        // The target directory itself is never changed; only the copy handed to the comparator.
        assertTrue(Fixtures.text(target.resolve(SYSTRAN)).contains(LATER_TS));
    }

    @Test
    void exactRule_leavesTheTimestampsForTheComparator() throws IOException {
        Path target = copyOfExpected("target");
        replaceInTransactions(target, LEGACY_TS, LATER_TS);

        SideBySide.Prepared prepared = SideBySide.prepare(Fixtures.expected(BASELINE), target, temp.resolve("work"), BASELINE, TimestampRule.EXACT);

        assertEquals(4, prepared.timestampsWellFormed());
        assertTrue(Fixtures.text(prepared.target().resolve(SYSTRAN)).contains(LATER_TS));
        assertFalse(Fixtures.text(prepared.target().resolve(SYSTRAN)).contains(LEGACY_TS));
    }

    @Test
    void formatRule_malformedTimestamp_staysADifference() throws IOException {
        Path target = copyOfExpected("target");
        replaceInTransactions(target, LEGACY_TS, "2022-07-05 10:00:00.000000");

        SideBySide.Prepared prepared = SideBySide.prepare(Fixtures.expected(BASELINE), target, temp.resolve("work"), BASELINE, TimestampRule.FORMAT);

        assertEquals(0, prepared.timestampsWellFormed());
        assertEquals(4, prepared.timestampsMalformed());
        assertTrue(Fixtures.text(prepared.target().resolve(SYSTRAN)).contains("2022-07-05 10:00:00.000000"));
    }

    @Test
    void prepare_laysTheLegacyDirectoryOutForTheComparator_withAManifestWhenItHasNone() throws IOException {
        Path legacy = copyOfExpected("legacy-run");
        Files.delete(legacy.resolve("manifest.json"));

        SideBySide.Prepared prepared = SideBySide.prepare(legacy, copyOfExpected("target"), temp.resolve("work"), "cycle-1", TimestampRule.FORMAT);

        Path expected = prepared.legacyRoot().resolve("interest/cycle-1/expected");
        assertEquals(Fixtures.text(Fixtures.expected(BASELINE).resolve("manifest.json")), Fixtures.text(expected.resolve("manifest.json")));
        assertEquals(Fixtures.text(legacy.resolve(SYSTRAN)), Fixtures.text(expected.resolve(SYSTRAN)));
        assertFalse(Files.exists(legacy.resolve("manifest.json")));
    }

    @Test
    void prepare_missingDirectory_isAnError() {
        assertThrows(IOException.class, () -> SideBySide.prepare(temp.resolve("nowhere"), temp, temp.resolve("work"), "x", TimestampRule.FORMAT));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "TRAN-PROC-TS; 2022-07-05-10.00.00.000000; 2022-07-06-01.02.03.450000; EXACT; INT-Q03",
            "TRAN-ORIG-TS; 2022-07-05-10.00.00.000000; 2022-07-06 01:02:03.450000; EXACT; defect",
            "TRAN-ORIG-TS; 2022-07-05-10.00.00.000000; 2022-07-06 01:02:03.450000; FORMAT; defect",
            "TRAN-ORIG-TS; 2022-07-05-10.00.00.000000; 2022-07-06-01.02.03.456789; EXACT; defect",
            "TRAN-ID; 2022071800000001; 2022081800000001; FORMAT; INT-Q02",
            "TRAN-ID; 2022071800000001; 2022071800000002; FORMAT; defect",
            "TRAN-AMT; 12.35; 12.36; FORMAT; INT-Q01",
            "TRAN-AMT; -3.00; -3.01; FORMAT; INT-Q01",
            "TRAN-AMT; 12.35; 12.37; FORMAT; defect",
            "TRAN-AMT; 12.35; None; FORMAT; defect",
            "TRAN-DESC; Int. for a/c 00000002001; Int. for a/c 00000002001   xx; FORMAT; G05",
            "TRAN-DESC; Int. for a/c 00000002001; Int. for a/c 00000002002; FORMAT; defect",
            "ACCT-CURR-BAL; 2400.00; 2437.00; FORMAT; defect",
            "TRAN-CARD-NUM; 4000000000002001; 4000000000002002; FORMAT; defect",
            "(record count); 2; 1; FORMAT; defect",
            "(text); ; -END OF EXECUTION; FORMAT; defect"})
    void everyDifference_getsItsClass(String field, String legacy, String target, TimestampRule rule, String expectedClass) {
        SideBySide.Classified classified = SideBySide.classify("somewhere", field, legacy == null ? "" : legacy, target, rule);

        assertEquals(expectedClass, classified.classId());
        assertFalse(classified.why().isBlank());
    }

    @Test
    void comparatorReport_everyDifferenceLineIsClassified() {
        String report = """
                ## interest / i01-baseline — java: **FAIL (4 differences)**

                Checked 2 datasets, 4 records, 50 decoded fields, 1 SYSOUT files, 0 table snapshots, return codes.

                | Where | Field | Expected (legacy) | Actual (candidate) |
                |---|---|---|---|
                | AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001) | `TRAN-AMT` | 10.00 | 10.01 |
                | AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001) | `TRAN-PROC-TS` | 2022-07-05-10.00.00.000000 | 2022-07-06-01.02.03.450000 |
                | AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS rec 2 (ACCT-ID=2002) | `ACCT-CURR-BAL` | 2400.00 | 2437.00 |
                | rc.json | `(text)` |  | -   "STEP15": 0,<br>+   "STEP15": 12, |
                | … | | 3 more | |

                Fields differing: `(text)`, `ACCT-CURR-BAL`, `TRAN-AMT`, `TRAN-PROC-TS`
                """;

        List<SideBySide.Classified> differences = SideBySide.classify(report, TimestampRule.EXACT);

        assertEquals(List.of("INT-Q01", "INT-Q03", "defect", "defect", "defect"),
                differences.stream().map(SideBySide.Classified::classId).toList());
        assertEquals("3 more", differences.get(4).target());
        String markdown = SideBySide.markdown(BASELINE, "legacy-dir", "target-dir", TimestampRule.EXACT, 4, 0, differences);
        assertTrue(markdown.contains("Timestamp rule used (SS3)"));
        assertTrue(markdown.contains("exact: the timestamps of the two runs must be equal"));
        assertTrue(markdown.contains("5 difference(s): 3 defect(s), 2 classified under a decision or a gap"));
        assertEquals(5, markdown.lines().filter(line -> line.matches(".*\\| \\*\\*(defect|INT-Q0[1-4]|G0[0-9])\\*\\* \\|.*")).count());
    }

    @Test
    void comparatorReportWithoutDifferences_classifiesNothing() {
        assertEquals(List.of(), SideBySide.classify("## interest / i01-baseline — java: **PASS**\n\nChecked 2 datasets.\n", TimestampRule.FORMAT));
    }

    @Test
    void kit_prepareAndReport_exitCodeSaysWhetherThereIsADefect() throws IOException {
        Path target = copyOfExpected("target");
        Path work = temp.resolve("work");
        assertEquals(0, DataMoveKit.run(new String[] {"sidebyside-prepare", "legacy=" + Fixtures.expected(BASELINE), "target=" + target,
                "work=" + work, "name=" + BASELINE, "timestamps=exact"}));
        String[] reportArguments = {"sidebyside-report", "compare=" + work.resolve("compare.md"), "prepared=" + work.resolve("prepared.txt"),
                "report=" + work.resolve("report.md"), "name=" + BASELINE, "legacy=l", "target=t", "timestamps=exact"};

        Files.writeString(work.resolve("compare.md"), "| a rec 1 | `TRAN-PROC-TS` | " + LEGACY_TS + " | " + LATER_TS + " |\n");
        assertEquals(0, DataMoveKit.run(reportArguments));
        assertTrue(Files.readString(work.resolve("report.md")).contains("**INT-Q03**"));

        Files.writeString(work.resolve("compare.md"), "| a rec 1 | `TRAN-CARD-NUM` | 1 | 2 |\n");
        assertEquals(1, DataMoveKit.run(reportArguments));
        assertTrue(Files.readString(work.resolve("report.md")).contains("**defect**"));

        Files.delete(work.resolve("compare.md"));
        assertEquals(1, DataMoveKit.run(reportArguments));
    }

    @Test
    void kit_usageErrors() {
        assertEquals(2, DataMoveKit.run(new String[] {}));
        assertEquals(2, DataMoveKit.run(new String[] {"something-else"}));
        assertEquals(2, DataMoveKit.run(new String[] {"sidebyside-prepare", "legacy=a"}));
        assertEquals(2, DataMoveKit.run(new String[] {"sidebyside-report", "timestamps=sometimes"}));
    }

    @Test
    void timestampRule_namesAndForm() {
        assertEquals(TimestampRule.FORMAT, TimestampRule.of(null));
        assertEquals(TimestampRule.FORMAT, TimestampRule.of("format"));
        assertEquals(TimestampRule.EXACT, TimestampRule.of("Exact"));
        assertThrows(IllegalArgumentException.class, () -> TimestampRule.of("sometimes"));
        assertTrue(TimestampRule.wellFormed(LEGACY_TS));
        assertTrue(TimestampRule.wellFormed(LATER_TS));
        assertFalse(TimestampRule.wellFormed("2022-07-05-10.00.00.000001"));
        assertFalse(TimestampRule.wellFormed("2022-07-05-10.00.00.00"));
        assertFalse(TimestampRule.wellFormed(""));
    }
}
