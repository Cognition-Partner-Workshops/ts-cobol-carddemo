package com.carddemo.interest.domain;

/**
 * The one exception that stops a run (INT-R08, standards S6.2). It is never caught to continue.
 * Its message never holds an account id, card number, amount or record image (standards S6.5).
 */
public class InterestRunStoppedException extends RuntimeException {

    private final StopReason reason;
    private final LegacyFile file;

    public InterestRunStoppedException(StopReason reason, String detail) {
        this(reason, detail, null);
    }

    public InterestRunStoppedException(StopReason reason, String detail, Throwable cause) {
        this(reason, null, detail, cause);
    }

    /** OPEN_INPUT carries the file that could not be opened as data (standards section 6). */
    public InterestRunStoppedException(StopReason reason, LegacyFile file, String detail, Throwable cause) {
        super(reason + " (" + reason.ruleId() + "): " + detail, cause);
        this.reason = reason;
        this.file = file;
    }

    /** The file of an OPEN_INPUT stop; null for every other reason and where the adapter did not name one. */
    public LegacyFile file() {
        return file;
    }

    public StopReason reason() {
        return reason;
    }
}
