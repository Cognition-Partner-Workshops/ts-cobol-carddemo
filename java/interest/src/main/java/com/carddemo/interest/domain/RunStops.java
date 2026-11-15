package com.carddemo.interest.domain;

import com.carddemo.interest.port.RunLog;

/**
 * INT-R08: every stop of the run writes what CBACT04C displays before it abends - the message of
 * the operation, the status line where the condition determines the status, and ABENDING PROGRAM
 * (CBACT04C:628-648) - and returns the one exception that ends the step. The account id of a
 * missing record goes to the run log only, as the legacy displays it, never into the exception
 * (standards S6.5). Nothing follows ABENDING PROGRAM: the line of the off-host abend stub is not
 * reproduced.
 */
public final class RunStops {

    private static final String ACCOUNT_NOT_FOUND = "ACCOUNT NOT FOUND: ";
    /** CBACT04C:635-648 for a record that is not on the file: status 23. */
    private static final String STATUS_RECORD_MISSING = "FILE STATUS IS: NNNN0023";
    private static final String ABENDING = "ABENDING PROGRAM";

    private final RunLog runLog;

    public RunStops(RunLog runLog) {
        this.runLog = runLog;
    }

    /** CBACT04C:373-390. */
    public InterestRunStoppedException accountMissing(long accountId) {
        runLog.line(ACCOUNT_NOT_FOUND + elevenDigits(accountId));
        return recordMissing(StopReason.ACCOUNT_MISSING, "a balance names an account that is not on the account file");
    }

    /** CBACT04C:394-412: the legacy displays the same first line as for a missing account. */
    public InterestRunStoppedException cardMissing(long accountId) {
        runLog.line(ACCOUNT_NOT_FOUND + elevenDigits(accountId));
        return recordMissing(StopReason.CARD_MISSING, "an account has no card cross-reference");
    }

    /** CBACT04C:443-459. */
    public InterestRunStoppedException defaultRateMissing() {
        return recordMissing(StopReason.DEFAULT_RATE_MISSING,
                "neither the account's group nor DEFAULT has a rate for a balance");
    }

    /** INT-H03: no legacy text exists; the line names the open decision and the account, as the legacy names it for a missing card. */
    public InterestRunStoppedException intH03SeveralCards(long accountId) {
        runLog.line("INT-H03 ACCOUNT HAS SEVERAL CARDS: " + elevenDigits(accountId));
        runLog.line(ABENDING);
        return new InterestRunStoppedException(StopReason.MULTI_CARD,
                "an account has several cards and interest.policy.multiCard is STOP");
    }

    /** INT-H05: no legacy text exists; the line names the open decision and the field, never the value. */
    public InterestRunStoppedException intH05DoesNotFit(String field) {
        runLog.line("INT-H05 VALUE DOES NOT FIT FIELD: " + field);
        runLog.line(ABENDING);
        return new InterestRunStoppedException(StopReason.OVERFLOW,
                field + " does not fit its field and interest.policy.overflow is STOP; nothing is truncated");
    }

    /**
     * An operation that failed for a reason the legacy status does not determine (a database or
     * file-system failure): the legacy message of the operation it replaces, no status line
     * (standards S6.1).
     */
    public InterestRunStoppedException operationFailed(StopReason reason, String detail) {
        reason.legacyMessage().ifPresent(runLog::line);
        runLog.line(ABENDING);
        return new InterestRunStoppedException(reason, detail);
    }

    private InterestRunStoppedException recordMissing(StopReason reason, String detail) {
        runLog.line(reason.legacyMessage().orElseThrow());
        runLog.line(STATUS_RECORD_MISSING);
        runLog.line(ABENDING);
        return new InterestRunStoppedException(reason, detail);
    }

    private static String elevenDigits(long accountId) {
        return String.format("%011d", accountId);
    }
}
