package com.carddemo.interest.recon;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Gives every difference of a side-by-side comparison its class (strategy SS4): {@code defect}, the
 * id of an open decision or the id of a mapping gap. A class other than defect is given only when
 * the two values themselves show the decision or the gap; everything else is a defect. The class
 * is a proposal for the test lead: the kit reports, it does not decide (REL-04, REL-05).
 */
public final class DifferenceClasses {

    public static final String DEFECT = "defect";

    /** INT-T05: YYYY-MM-DD-HH.MM.SS.hh0000. */
    private static final Pattern TIMESTAMP = Pattern.compile("\\d{4}-\\d{2}-\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{2}0000");
    private static final Pattern AMOUNT = Pattern.compile("-?\\d+\\.\\d{2}");
    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");
    private static final int PARM_LENGTH = 10;
    private static final int ID_LENGTH = 16;

    /** A difference with its class, why, and whether the chosen timestamp rule accepts it. */
    public record Classified(Difference difference, String differenceClass, String why, boolean isAccepted) {
    }

    private final TimestampRule timestampRule;

    public DifferenceClasses(TimestampRule timestampRule) {
        this.timestampRule = timestampRule;
    }

    public Classified classify(Difference difference) {
        return switch (difference.field()) {
            case "TRAN-ORIG-TS", "TRAN-PROC-TS" -> intQ03Timestamp(difference);
            case "TRAN-AMT" -> intQ01OneCent(difference);
            case "TRAN-ID" -> intQ02IdPrefix(difference);
            case "TRAN-DESC" -> g05UnsetTail(difference);
            default -> defect(difference, "no decision or gap explains a difference in this field");
        };
    }

    // INT-Q03: both timestamps are the clock of their own run
    private Classified intQ03Timestamp(Difference difference) {
        if (!TIMESTAMP.matcher(difference.legacy()).matches() || !TIMESTAMP.matcher(difference.target()).matches()) {
            return defect(difference, "a value does not have the format YYYY-MM-DD-HH.MM.SS.hh0000 (INT-T05)");
        }
        return timestampRule == TimestampRule.FORMAT
                ? new Classified(difference, "INT-Q03", "both values have the format; the clocks of the two runs differ", true)
                : new Classified(difference, "INT-Q03", "the values differ although the comparison is exact: the target run did not have the legacy clock", false);
    }

    // INT-Q01: truncation against rounding moves an amount by one cent
    private Classified intQ01OneCent(Difference difference) {
        if (AMOUNT.matcher(difference.legacy()).matches() && AMOUNT.matcher(difference.target()).matches()
                && new BigDecimal(difference.legacy()).subtract(new BigDecimal(difference.target())).abs().compareTo(ONE_CENT) == 0) {
            return new Classified(difference, "INT-Q01", "the amounts are one cent apart: truncated against rounded", false);
        }
        return defect(difference, "the amounts are not one cent apart");
    }

    // INT-Q02: the id is the PARM text and a counter; another PARM gives another prefix
    private Classified intQ02IdPrefix(Difference difference) {
        if (difference.legacy().length() == ID_LENGTH && difference.target().length() == ID_LENGTH
                && difference.legacy().substring(PARM_LENGTH).equals(difference.target().substring(PARM_LENGTH))) {
            return new Classified(difference, "INT-Q02", "the counters are the same; the first ten characters (the PARM) differ", false);
        }
        return defect(difference, "the counters of the two ids differ");
    }

    // G05: the legacy never sets the tail of TRAN-DESC; the target writes spaces
    private Classified g05UnsetTail(Difference difference) {
        if (visible(difference.legacy()).equals(visible(difference.target()))) {
            return new Classified(difference, "G05", "the texts are the same; the bytes after them differ (unset on the legacy side)", false);
        }
        return defect(difference, "the texts differ");
    }

    private static String visible(String text) {
        return text.replace('\u0000', ' ').stripTrailing();
    }

    private static Classified defect(Difference difference, String why) {
        return new Classified(difference, DEFECT, why, false);
    }
}
