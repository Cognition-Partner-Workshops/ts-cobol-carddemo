package com.carddemo.interest.recon;

/**
 * How TRAN-ORIG-TS and TRAN-PROC-TS of two runs are compared (strategy SS3). The two runs happen
 * at different moments (INT-Q03), so either the format alone is compared, or the target was given
 * the legacy run's clock and the values must be the same. The kit offers both; choosing is not its decision.
 */
public enum TimestampRule {
    FORMAT("format", "TRAN-ORIG-TS and TRAN-PROC-TS are compared for format only (YYYY-MM-DD-HH.MM.SS.hh0000): "
            + "two values of that format that differ are listed with class INT-Q03 and accepted"),
    EXACT("exact", "TRAN-ORIG-TS and TRAN-PROC-TS are compared exactly: the target run is expected to have had the "
            + "legacy run's clock, and a value that differs is a difference of class INT-Q03");

    private final String parameter;
    private final String sentence;

    TimestampRule(String parameter, String sentence) {
        this.parameter = parameter;
        this.sentence = sentence;
    }

    /** The value of the argument {@code timestamps}: {@code format} or {@code exact}; anything else is refused. */
    public static TimestampRule of(String parameter) {
        for (TimestampRule rule : values()) {
            if (rule.parameter.equals(parameter)) {
                return rule;
            }
        }
        throw new IllegalArgumentException("timestamps must be format or exact");
    }

    public String parameter() {
        return parameter;
    }

    /** The rule as it is written into the comparison report (SS3). */
    public String sentence() {
        return sentence;
    }
}
