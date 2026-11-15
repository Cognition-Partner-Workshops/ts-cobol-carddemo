package com.carddemo.interest.recon;

import java.util.Locale;

/**
 * How TRAN-ORIG-TS and TRAN-PROC-TS are compared side by side (strategy item SS3). The two runs
 * happen at different moments (rule INT-Q03), so either only the form of the timestamp is compared,
 * or the target was given the legacy run's clock and the values must be the same. Which rule is
 * the right one is not decided here; both are offered and the report names the one used.
 */
public enum TimestampRule {

    FORMAT("format only: both values must have the form YYYY-MM-DD-HH.MM.SS.hh0000; their difference is the run clock (INT-Q03)"),
    EXACT("exact: the target ran on the legacy run's clock, so any difference is a defect");

    private final String description;

    TimestampRule(String description) {
        this.description = description;
    }

    public static TimestampRule ofParameter(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    public String description() {
        return description;
    }
}
