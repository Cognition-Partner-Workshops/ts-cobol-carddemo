package com.carddemo.interest.domain;

import java.util.Optional;

/**
 * Why a run stopped. The legacy has one failure path (INT-R08, CBACT04C:628-632); so has the target.
 * A reason that replaces a legacy abend carries the message CBACT04C displays for it; the reasons of
 * the two holes (INT-H03, INT-H05) and of the target's own boundaries have no legacy text.
 */
public enum StopReason {
    /** An input file cannot be opened (CBACT04C:234-323). */
    OPEN_INPUT("INT-R08"),
    /** A record breaks encoding rule E1, E2 or E3 at the boundary (standards S3.5). */
    RECORD_REJECTED("INT-R08"),
    /** The category balances cannot be read (CBACT04C:341-345). */
    READ_BALANCES("INT-R08", "ERROR READING TRANSACTION CATEGORY FILE"),
    /** No account record for a balance (CBACT04C:373-390). */
    ACCOUNT_MISSING("INT-R08", "ERROR READING ACCOUNT FILE"),
    /** The account cannot be read (CBACT04C:378-390, a status other than 00 and 23). */
    READ_ACCOUNT("INT-R08", "ERROR READING ACCOUNT FILE"),
    /** No card cross-reference for the account (CBACT04C:394-412). */
    CARD_MISSING("INT-R08", "ERROR READING XREF FILE"),
    /** The card cross-reference cannot be read (CBACT04C:400-412, a status other than 00 and 23). */
    READ_CARD("INT-R08", "ERROR READING XREF FILE"),
    /** Several cards for one account: the source does not determine the outcome (INT-H03). */
    MULTI_CARD("INT-H03"),
    /** The rates cannot be read (CBACT04C:422-434). */
    READ_RATE("INT-R08", "ERROR READING DISCLOSURE GROUP FILE"),
    /** Neither the account's group nor DEFAULT has a rate for the balance (CBACT04C:443-459). */
    DEFAULT_RATE_MISSING("INT-R08", "ERROR READING DEFAULT DISCLOSURE GROUP"),
    /** inactiveAccounts=SKIP needs the active flag of the account and the adapter does not supply it (INT-H04). */
    ACTIVE_FLAG_NOT_SUPPLIED("INT-H04"),
    /** An amount or the transaction counter does not fit its field: the source does not determine the outcome (INT-H05). */
    OVERFLOW("INT-H05"),
    /** The account of an account break cannot be updated (CBACT04C:356-369). */
    UPDATE_ACCOUNT("INT-R08", "ERROR RE-WRITING ACCOUNT FILE"),
    /** An interest transaction cannot be written (CBACT04C:500-514). */
    WRITE_TRANSACTION("INT-R08", "ERROR WRITING TRANSACTION RECORD"),
    /** The outputs cannot be written or moved into place (CBACT04C:522-611). */
    WRITE_OUTPUT("INT-R08");

    private final String ruleId;
    private final Optional<String> legacyMessage;

    StopReason(String ruleId) {
        this.ruleId = ruleId;
        this.legacyMessage = Optional.empty();
    }

    StopReason(String ruleId, String legacyMessage) {
        this.ruleId = ruleId;
        this.legacyMessage = Optional.of(legacyMessage);
    }

    public String ruleId() {
        return ruleId;
    }

    /** The line CBACT04C displays before the status line and the abend; empty where the legacy has none. */
    public Optional<String> legacyMessage() {
        return legacyMessage;
    }
}
