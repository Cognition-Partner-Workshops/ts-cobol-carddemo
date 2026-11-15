package com.carddemo.interest.domain;

/**
 * Why a run stopped. The legacy has one failure path (INT-R08, CBACT04C:628-632); so has the target.
 * Each reason carries the line CBACT04C displays for it and, where the condition determines the
 * legacy file status (a missing record is 23), that status (standards section 6, S6.1).
 */
public enum StopReason {
    /** A file, or the table that replaces it, cannot be opened (CBACT04C:234-323); the text depends on the file. */
    OPEN_INPUT("INT-R08", null, null),
    /** A record breaks encoding rule E1, E2 or E3 at the boundary (standards S3.5). */
    RECORD_REJECTED("INT-R08", null, null),
    /** The balances cannot be read (CBACT04C:341-345). */
    READ_BALANCES("INT-R08", "ERROR READING TRANSACTION CATEGORY FILE", null),
    /** No account record for a balance (CBACT04C:373-390). */
    ACCOUNT_MISSING("INT-R08", "ERROR READING ACCOUNT FILE", "23"),
    /** The accounts cannot be read (CBACT04C:378-390, a status other than 23). */
    READ_ACCOUNT("INT-R08", "ERROR READING ACCOUNT FILE", null),
    /** No card cross-reference for the account (CBACT04C:394-412). */
    CARD_MISSING("INT-R08", "ERROR READING XREF FILE", "23"),
    /** The card cross-reference cannot be read (CBACT04C:400-412, a status other than 23). */
    READ_CARD("INT-R08", "ERROR READING XREF FILE", null),
    /** Several cards for one account: the source does not determine the outcome (INT-H03, G08). */
    MULTI_CARD("INT-H03", "INT-H03: THE ACCOUNT HAS SEVERAL CARDS", null),
    /** The rates cannot be read (CBACT04C:422-434). */
    READ_RATE("INT-R08", "ERROR READING DISCLOSURE GROUP FILE", null),
    /** Neither the account's group nor the group DEFAULT has a rate for a balance (CBACT04C:444-459). */
    DEFAULT_RATE_MISSING("INT-R08", "ERROR READING DEFAULT DISCLOSURE GROUP", "23"),
    /** timestampSource=CYCLE_DATE and the PARM does not start with eight digits (INT-Q03 option B). */
    CYCLE_DATE_UNREADABLE("INT-Q03", null, null),
    /** An amount or the transaction counter does not fit its field: the source does not determine the outcome (INT-H05). */
    OVERFLOW("INT-H05", "INT-H05: A VALUE DOES NOT FIT ITS FIELD", null),
    /** The account cannot be updated (CBACT04C:356-369). */
    UPDATE_ACCOUNT("INT-R08", "ERROR RE-WRITING ACCOUNT FILE", null),
    /** A transaction cannot be written (CBACT04C:500-514). */
    WRITE_TRANSACTION("INT-R08", "ERROR WRITING TRANSACTION RECORD", null),
    /** The outputs cannot be written or moved into place (CBACT04C:500-514, 522-611). */
    WRITE_OUTPUT("INT-R08", null, null);

    private final String ruleId;
    private final String legacyMessage;
    private final String legacyFileStatus;

    StopReason(String ruleId, String legacyMessage, String legacyFileStatus) {
        this.ruleId = ruleId;
        this.legacyMessage = legacyMessage;
        this.legacyFileStatus = legacyFileStatus;
    }

    public String ruleId() {
        return ruleId;
    }

    /** The line the run log gets for this reason; null where the text depends on the file or none exists yet. */
    String legacyMessage() {
        return legacyMessage;
    }

    /** The two-character legacy file status, only where the condition determines it; else null. */
    String legacyFileStatus() {
        return legacyFileStatus;
    }
}
