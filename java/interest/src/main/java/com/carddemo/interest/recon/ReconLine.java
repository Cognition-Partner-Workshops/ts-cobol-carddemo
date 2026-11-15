package com.carddemo.interest.recon;

/** One line of the reconciliation report: a check, its two figures and the verdict. */
public record ReconLine(String check, String what, String leftSource, Figure left, String rightSource, Figure right,
        Verdict verdict, String note) {

    public static ReconLine compare(String check, String what, String leftSource, Figure left, String rightSource, Figure right) {
        return compare(check, what, leftSource, left, rightSource, right, "");
    }

    public static ReconLine compare(String check, String what, String leftSource, Figure left, String rightSource, Figure right,
            String note) {
        Verdict verdict = !left.available() || !right.available() ? Verdict.NOT_AVAILABLE
                : left.sameAs(right) ? Verdict.EQUAL : Verdict.DIFFERENT;
        return new ReconLine(check, what, leftSource, left, rightSource, right, verdict, note);
    }

    public static ReconLine notAvailable(String check, String what, String why) {
        return new ReconLine(check, what, "", Figure.missing(why), "", Figure.missing(why), Verdict.NOT_AVAILABLE, "");
    }
}
