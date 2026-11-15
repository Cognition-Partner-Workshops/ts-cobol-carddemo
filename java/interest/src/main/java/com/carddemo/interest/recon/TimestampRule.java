package com.carddemo.interest.recon;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * How the side-by-side run compares TRAN-ORIG-TS and TRAN-PROC-TS (strategy SS3). The two runs
 * happen at different moments (INT-Q03), so either the format only is compared, or the values are
 * compared exactly because the target was given the legacy run's clock. The kit offers both and
 * writes the one used into the report; choosing is not its decision.
 */
public enum TimestampRule {
    FORMAT("format only: a timestamp of the target is accepted when it has the form YYYY-MM-DD-HH.MM.SS.hh0000 (G10), whatever its value"),
    EXACT("exact: the timestamps of the two runs must be equal (the target ran with the legacy run's clock)");

    /** CVTRA05Y TRAN-ORIG-TS / TRAN-PROC-TS as CBACT04C writes them: hundredths, then four zeros (G10). */
    private static final Pattern FORM = Pattern.compile("\\d{4}-\\d{2}-\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{2}0000");

    private final String description;

    TimestampRule(String description) {
        this.description = description;
    }

    public static TimestampRule of(String value) {
        if (value == null || value.isBlank()) {
            return FORMAT;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("timestamps must be format or exact, not " + value, e);
        }
    }

    public String description() {
        return description;
    }

    public static boolean wellFormed(String timestamp) {
        return FORM.matcher(timestamp).matches();
    }
}
