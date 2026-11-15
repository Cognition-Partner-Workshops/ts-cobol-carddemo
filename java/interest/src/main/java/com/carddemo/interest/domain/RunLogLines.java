package com.carddemo.interest.domain;

/** The fixed lines CBACT04C displays on a normal run (INT-T02, CBACT04C:181, 230, 418-419). */
public final class RunLogLines {

    public static final String START = "START OF EXECUTION OF PROGRAM CBACT04C";
    public static final String END = "END OF EXECUTION OF PROGRAM CBACT04C";
    /** INT-R03: displayed when the account's group has no rate for a balance (CBACT04C:418-419). */
    public static final String GROUP_RATE_MISSING = "DISCLOSURE GROUP RECORD MISSING";
    public static final String TRY_DEFAULT_GROUP = "TRY WITH DEFAULT GROUP CODE";

    private RunLogLines() {
    }
}
