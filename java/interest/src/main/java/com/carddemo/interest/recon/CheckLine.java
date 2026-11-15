package com.carddemo.interest.recon;

/**
 * One line of the reconciliation report: the check (RC1 to RC6 of strategy-interest.md), what is
 * compared, the two figures with the side each comes from, the result and a note.
 */
public record CheckLine(String check, String what, String firstSide, Figure first, String secondSide, Figure second,
        Verdict verdict, String note) {

    /** EQUAL when both figures are available and the same, DIFFERENT when both are available and not. */
    public static CheckLine compare(String check, String what, String firstSide, Figure first, String secondSide,
            Figure second, String note) {
        return new CheckLine(check, what, firstSide, first, secondSide, second, verdictOf(first, second, true), note);
    }

    /** As {@link #compare}, and DIFFERENT as well when a condition beside the two figures does not hold. */
    public static CheckLine compare(String check, String what, String firstSide, Figure first, String secondSide,
            Figure second, boolean holdsBeside, String note) {
        return new CheckLine(check, what, firstSide, first, secondSide, second, verdictOf(first, second, holdsBeside), note);
    }

    private static Verdict verdictOf(Figure first, Figure second, boolean holdsBeside) {
        if (!first.isAvailable() || !second.isAvailable()) {
            return Verdict.NOT_AVAILABLE;
        }
        return first.sameAs(second) && holdsBeside ? Verdict.EQUAL : Verdict.DIFFERENT;
    }
}
