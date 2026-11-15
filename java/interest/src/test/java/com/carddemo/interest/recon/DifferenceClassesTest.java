package com.carddemo.interest.recon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.carddemo.interest.recon.DifferenceClasses.Classified;

/**
 * Strategy SS3 and SS4: the timestamp rule that was chosen and the class of every difference.
 * The values are written by hand in the form tools/parity/compare.py prints them; the legacy
 * timestamp is the one of the recorded cases (fixtures/interest/i01-baseline/expected).
 */
class DifferenceClassesTest {

    private static final String WHERE = "AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001)";
    private static final String LEGACY_TS = "2022-07-05-10.00.00.000000";

    private static Classified classified(TimestampRule rule, String field, String legacy, String target) {
        return new DifferenceClasses(rule).classify(new Difference(WHERE, field, legacy, target));
    }

    @Test
    void intQ03_timestampsComparedForFormatOnly_anotherClockIsAccepted() {
        for (String field : new String[] {"TRAN-ORIG-TS", "TRAN-PROC-TS"}) {
            Classified classified = classified(TimestampRule.FORMAT, field, LEGACY_TS, "2022-08-05-11.00.00.120000");

            assertEquals("INT-Q03", classified.differenceClass());
            assertTrue(classified.isAccepted());
        }
    }

    @Test
    void intQ03_timestampsComparedExactly_anotherClockIsADifferenceOfTheDecision() {
        Classified classified = classified(TimestampRule.EXACT, "TRAN-ORIG-TS", LEGACY_TS, "2022-08-05-11.00.00.120000");

        assertEquals("INT-Q03", classified.differenceClass());
        assertFalse(classified.isAccepted());
    }

    @Test
    void intQ03_aTimestampOfAnotherFormatIsADefect_underBothRules() {
        // ISO form instead of YYYY-MM-DD-HH.MM.SS.hh0000, and hundredths that are not followed by 0000 (INT-T05)
        for (TimestampRule rule : TimestampRule.values()) {
            assertEquals(DifferenceClasses.DEFECT, classified(rule, "TRAN-PROC-TS", LEGACY_TS, "2022-08-05T11:00:00.12").differenceClass());
            assertEquals(DifferenceClasses.DEFECT, classified(rule, "TRAN-PROC-TS", LEGACY_TS, "2022-08-05-11.00.00.123456").differenceClass());
            assertFalse(classified(rule, "TRAN-PROC-TS", LEGACY_TS, "2022-08-05T11:00:00.12").isAccepted());
        }
    }

    @Test
    void intQ03_theRuleIsNamedByItsArgument_andAnotherValueIsRefused() {
        assertEquals(TimestampRule.FORMAT, TimestampRule.of("format"));
        assertEquals(TimestampRule.EXACT, TimestampRule.of("exact"));
        assertThrows(IllegalArgumentException.class, () -> TimestampRule.of("ignore"));
        assertThrows(IllegalArgumentException.class, () -> TimestampRule.of(null));
    }

    @Test
    void intQ01_amountsOneCentApart_areTheRoundingDecision() {
        // i02-tie-half-cent: 1235.50 * 12.00 / 1200 = 12.355, truncated 12.35, rounded 12.36
        Classified classified = classified(TimestampRule.FORMAT, "TRAN-AMT", "12.35", "12.36");

        assertEquals("INT-Q01", classified.differenceClass());
        assertFalse(classified.isAccepted());
        assertEquals("INT-Q01", classified(TimestampRule.FORMAT, "TRAN-AMT", "-3.00", "-3.01").differenceClass());
    }

    @Test
    void intQ01_amountsFurtherApart_areADefect() {
        assertEquals(DifferenceClasses.DEFECT, classified(TimestampRule.FORMAT, "TRAN-AMT", "12.35", "12.37").differenceClass());
        assertEquals(DifferenceClasses.DEFECT, classified(TimestampRule.FORMAT, "TRAN-AMT", "12.35", "None").differenceClass());
    }

    @Test
    void intQ02_sameCounterAfterAnotherParm_isTheIdDecision_anotherCounterIsADefect() {
        assertEquals("INT-Q02", classified(TimestampRule.FORMAT, "TRAN-ID", "2022071800000001", "2022081800000001").differenceClass());
        assertEquals(DifferenceClasses.DEFECT, classified(TimestampRule.FORMAT, "TRAN-ID", "2022071800000001", "2022071800000002").differenceClass());
    }

    @Test
    void g05_descriptionWithAnUnsetTail_isTheGap_anotherTextIsADefect() {
        assertEquals("G05", classified(TimestampRule.FORMAT, "TRAN-DESC", "Int. for a/c 00000002001\u0000\u0000", "Int. for a/c 00000002001").differenceClass());
        assertEquals(DifferenceClasses.DEFECT, classified(TimestampRule.FORMAT, "TRAN-DESC", "Int. for a/c 00000002001", "Int. for a/c 00000002002").differenceClass());
    }

    @Test
    void everyOtherDifference_isADefect() {
        // the balance of the negative control, a missing file, the run log and the return codes
        assertEquals(DifferenceClasses.DEFECT, classified(TimestampRule.FORMAT, "ACCT-CURR-BAL", "1247.85", "1247.86").differenceClass());
        assertEquals(DifferenceClasses.DEFECT, classified(TimestampRule.FORMAT, "(file)", "2 records", "missing").differenceClass());
        assertEquals(DifferenceClasses.DEFECT, classified(TimestampRule.FORMAT, "(record count)", "2", "3").differenceClass());
        assertEquals(DifferenceClasses.DEFECT, classified(TimestampRule.FORMAT, "(text)", "", "@@ -1 +1 @@").differenceClass());
    }

    @Test
    void differenceLine_hasFourColumns_withTabsAndNewlinesEscaped() {
        Difference difference = Difference.ofLine("SYSOUT INTCALC.STEP15\t(text)\t\t@@ -2 +2 @@\\n-a\\\\b\\n+a\\tb");

        assertEquals(new Difference("SYSOUT INTCALC.STEP15", "(text)", "", "@@ -2 +2 @@\n-a\\b\n+a\tb"), difference);
        assertThrows(IllegalArgumentException.class, () -> Difference.ofLine("only\tthree\tcolumns"));
    }
}
