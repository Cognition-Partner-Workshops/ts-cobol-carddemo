package com.carddemo.interest.domain;

/**
 * Why a run stopped. The legacy has one failure path (INT-R08, CBACT04C:628-632); so has the target.
 * Only the reasons the code can raise are named; the legacy messages of each are ticket 08.
 */
public enum StopReason {
    /** An input file cannot be opened (CBACT04C:234-323). */
    OPEN_INPUT("INT-R08"),
    /** A record breaks encoding rule E1, E2 or E3 at the boundary (standards S3.5). */
    RECORD_REJECTED("INT-R08"),
    /** No account record for a balance (CBACT04C:373-390). */
    ACCOUNT_MISSING("INT-R08"),
    /** No card cross-reference for the account (CBACT04C:394-412). */
    CARD_MISSING("INT-R08"),
    /** Several cards for one account: the source does not determine the outcome (INT-H03). */
    MULTI_CARD("INT-H03"),
    /** Neither the account's group nor DEFAULT has a rate for the balance (CBACT04C:443-459). */
    DEFAULT_RATE_MISSING("INT-R08"),
    /** inactiveAccounts=SKIP needs the active flag of the account and the adapter does not supply it (INT-H04). */
    ACTIVE_FLAG_NOT_SUPPLIED("INT-H04"),
    /** The outputs cannot be written or moved into place (CBACT04C:500-514, 522-611). */
    WRITE_OUTPUT("INT-R08");

    private final String ruleId;

    StopReason(String ruleId) {
        this.ruleId = ruleId;
    }

    public String ruleId() {
        return ruleId;
    }
}
