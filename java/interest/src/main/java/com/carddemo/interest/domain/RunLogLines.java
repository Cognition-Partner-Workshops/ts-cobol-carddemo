package com.carddemo.interest.domain;

/**
 * The lines CBACT04C displays: those of a normal run (INT-T02, CBACT04C:181, 230, 418-419) and
 * those of its one failure path - message, status line, ABENDING PROGRAM (INT-R08, INT-T08,
 * CBACT04C:372-391, 393-413, 443-460, 628-648).
 */
public final class RunLogLines {

    public static final String START = "START OF EXECUTION OF PROGRAM CBACT04C";
    public static final String END = "END OF EXECUTION OF PROGRAM CBACT04C";
    /** INT-R03: the two lines of the fallback to the group DEFAULT (CBACT04C:418-419). */
    public static final String GROUP_RATE_MISSING = "DISCLOSURE GROUP RECORD MISSING";
    public static final String TRYING_DEFAULT_GROUP = "TRY WITH DEFAULT GROUP CODE";


    /** INT-R08 (CBACT04C:375, 397): followed by the eleven digits of the account id. */
    private static final String ACCOUNT_NOT_FOUND = "ACCOUNT NOT FOUND: ";
    public static final String ERROR_READING_ACCOUNT = "ERROR READING ACCOUNT FILE";
    public static final String ERROR_READING_CARD = "ERROR READING XREF FILE";
    public static final String ERROR_READING_DEFAULT_RATE = "ERROR READING DEFAULT DISCLOSURE GROUP";
    /** INT-T08 (CBACT04C:635-648): the status line of a record that is not there, file status 23. */
    public static final String STATUS_RECORD_MISSING = "FILE STATUS IS: NNNN0023";
    /** INT-T03 (CBACT04C:629): the last line of a run that stops. */
    public static final String ABENDING = "ABENDING PROGRAM";

    /** INT-H03 and INT-H05 have no legacy text: the source does not determine what the host does. */
    private static final String SEVERAL_CARDS = "INT-H03 ACCOUNT HAS SEVERAL CARDS: ";
    private static final String VALUE_DOES_NOT_FIT = "INT-H05 VALUE DOES NOT FIT ITS FIELD: ";

    public static String accountNotFound(long accountId) {
        return ACCOUNT_NOT_FOUND + elevenDigits(accountId);
    }

    public static String severalCards(long accountId) {
        return SEVERAL_CARDS + elevenDigits(accountId);
    }

    public static String valueDoesNotFit(String what) {
        return VALUE_DOES_NOT_FIT + what;
    }

    private static String elevenDigits(long accountId) {
        return String.format("%011d", accountId);
    }

    private RunLogLines() {
    }
}
