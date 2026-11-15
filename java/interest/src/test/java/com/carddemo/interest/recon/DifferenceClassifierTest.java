package com.carddemo.interest.recon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Every difference gets a class: defect, decision id or gap id (strategy SS3, SS4). Synthetic values only. */
class DifferenceClassifierTest {

    private static final String SYSTRAN = "AWS.M2.CARDDEMO.SYSTRAN.G0001V00 rec 1 (TRAN-ID=2022071800000001)";
    private static final String LEGACY_TS = "2022-07-05-10.00.00.000000";
    private static final String TARGET_TS = "2022-07-06-11.30.00.120000";

    private static DifferenceClass classify(TimestampRule rule, String where, String field, String legacy, String target) {
        return new DifferenceClassifier(rule, 2).classify(new ComparedDifference(where, field, legacy, target));
    }

    @Test
    void formatOnly_wellFormedTimestampsThatDifferAreAcceptedAsIntQ03() {
        for (String field : new String[] {"TRAN-ORIG-TS", "TRAN-PROC-TS"}) {
            DifferenceClass result = classify(TimestampRule.FORMAT, SYSTRAN, field, LEGACY_TS, TARGET_TS);

            assertEquals("decision INT-Q03", result.label());
            assertTrue(result.accepted());
        }
    }

    @Test
    void formatOnly_aMalformedTimestampIsADefect() {
        DifferenceClass result = classify(TimestampRule.FORMAT, SYSTRAN, "TRAN-PROC-TS", LEGACY_TS, "2022-07-06 11:30:00.120000");

        assertEquals("defect", result.label());
        assertFalse(result.accepted());
    }

    @Test
    void exact_timestampsThatDifferAreADefect() {
        DifferenceClass result = classify(TimestampRule.EXACT, SYSTRAN, "TRAN-ORIG-TS", LEGACY_TS, TARGET_TS);

        assertEquals("defect", result.label());
        assertFalse(result.accepted());
    }

    @Test
    void amountOneCentApartIsDecisionIntQ01_anyOtherAmountIsADefect() {
        assertEquals("decision INT-Q01", classify(TimestampRule.FORMAT, SYSTRAN, "TRAN-AMT", "12.35", "12.36").label());
        assertEquals("decision INT-Q01", classify(TimestampRule.FORMAT, SYSTRAN, "TRAN-AMT", "-3.00", "-3.01").label());
        assertEquals("defect", classify(TimestampRule.FORMAT, SYSTRAN, "TRAN-AMT", "12.35", "12.37").label());
        assertFalse(classify(TimestampRule.FORMAT, SYSTRAN, "TRAN-AMT", "12.35", "12.36").accepted());
    }

    @Test
    void transactionIdWithTheSameCounterAndAnotherPrefixIsDecisionIntQ02() {
        assertEquals("decision INT-Q02", classify(TimestampRule.FORMAT, SYSTRAN, "TRAN-ID", "2022071800000001", "2022071900000001").label());
        assertEquals("defect", classify(TimestampRule.FORMAT, SYSTRAN, "TRAN-ID", "2022071800000001", "2022071800000002").label());
    }

    @Test
    void accountBreakAmountsOnTheLastAccountRecordAreDecisionIntQ04_onAnyOtherRecordADefect() {
        String last = "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS rec 2 (ACCT-ID=2002)";
        String first = "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS rec 1 (ACCT-ID=2001)";

        assertEquals("decision INT-Q04", classify(TimestampRule.FORMAT, last, "ACCT-CURR-BAL", "2400.00", "2437.00").label());
        assertEquals("decision INT-Q04", classify(TimestampRule.FORMAT, last, "ACCT-CURR-CYC-CREDIT", "2400.00", "0.00").label());
        assertEquals("defect", classify(TimestampRule.FORMAT, first, "ACCT-CURR-BAL", "1010.00", "1000.00").label());
        assertEquals("defect", classify(TimestampRule.FORMAT, last, "ACCT-CREDIT-LIMIT", "6000.00", "6001.00").label());
    }

    @Test
    void accountBreakAmountIsADefectWhenTheLastRecordIsNotKnown() {
        DifferenceClass result = new DifferenceClassifier(TimestampRule.FORMAT, 0).classify(new ComparedDifference(
                "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS rec 2 (ACCT-ID=2002)", "ACCT-CURR-BAL", "2400.00", "2437.00"));

        assertEquals("defect", result.label());
    }

    @Test
    void descriptionThatAgreesAsFarAsTheTargetWroteItIsGapG05() {
        assertEquals("gap G05", classify(TimestampRule.FORMAT, SYSTRAN, "TRAN-DESC", "Int. for a/c 00000002001 XYZ", "Int. for a/c 00000002001").label());
        assertEquals("defect", classify(TimestampRule.FORMAT, SYSTRAN, "TRAN-DESC", "Int. for a/c 00000002001", "Int. for a/c 00000002002").label());
    }

    @Test
    void everythingElseIsADefect_nothingIsLeftWithoutAClass() {
        assertEquals("defect", classify(TimestampRule.FORMAT, SYSTRAN, "TRAN-CARD-NUM", "4000000000002001", "4000000000002002").label());
        assertEquals("defect", classify(TimestampRule.FORMAT, "SYSOUT INTCALC.STEP15", "(text)", "", "-END OF EXECUTION").label());
        assertEquals("defect", classify(TimestampRule.FORMAT, "rc.json", "(text)", "", "+ \"STEP15\": 12").label());
        assertEquals("defect", classify(TimestampRule.FORMAT, "AWS.M2.CARDDEMO.SYSTRAN.G0001V00", "(record count)", "2", "1").label());
    }
}
