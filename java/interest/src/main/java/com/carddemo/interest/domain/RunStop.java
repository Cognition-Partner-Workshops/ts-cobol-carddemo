package com.carddemo.interest.domain;

import com.carddemo.interest.port.RunLog;

/**
 * The one failure path (INT-R08, CBACT04C:628-648): the lines the legacy displays for a stop go to
 * the run log - the message of the reason, the status line where the legacy status is determined,
 * then ABENDING PROGRAM - and the one exception that ends the step is returned to be thrown.
 * Nothing is written after ABENDING PROGRAM: the abend stub's own line is not reproduced
 * (standards section 4, row z).
 */
public final class RunStop {

    static final String ABENDING = "ABENDING PROGRAM";
    /** INT-T08 (CBACT04C:643-646): the literal, then the status right-aligned in four digits. */
    private static final String STATUS_LINE = "FILE STATUS IS: NNNN00";
    /** CBACT04C:375, 396: displayed when a keyed read finds no record, for the account and for its card alike. */
    private static final String ACCOUNT_NOT_FOUND = "ACCOUNT NOT FOUND: ";

    private RunStop() {
    }

    public static InterestRunStoppedException of(RunLog runLog, StopReason reason, String detail) {
        return of(runLog, reason, detail, null);
    }

    public static InterestRunStoppedException of(RunLog runLog, StopReason reason, String detail, Throwable cause) {
        if (reason.legacyMessage() != null) {
            runLog.line(reason.legacyMessage());
        }
        if (reason.legacyFileStatus() != null) {
            runLog.line(statusLine(reason.legacyFileStatus()));
        }
        runLog.line(ABENDING);
        return new InterestRunStoppedException(reason, detail, cause);
    }

    /** A keyed read by account id found nothing: the legacy names the id first (CBACT04C:375, 396). */
    static InterestRunStoppedException recordMissing(RunLog runLog, StopReason reason, long accountId, String detail) {
        runLog.line(ACCOUNT_NOT_FOUND + AccountIdText.elevenDigits(accountId));
        return of(runLog, reason, detail);
    }

    /** A file, or what replaces it, cannot be opened: the text is that of the file (INT-T10 for the rates). */
    public static InterestRunStoppedException openFailed(RunLog runLog, LegacyFile file, String detail, Throwable cause) {
        runLog.line(file.openFailedMessage());
        runLog.line(ABENDING);
        return new InterestRunStoppedException(StopReason.OPEN_INPUT, file, detail, cause);
    }

    static String statusLine(String twoCharacterStatus) {
        return STATUS_LINE + twoCharacterStatus;
    }
}
