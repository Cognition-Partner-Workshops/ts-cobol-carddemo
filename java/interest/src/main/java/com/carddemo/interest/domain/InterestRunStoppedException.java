package com.carddemo.interest.domain;

/**
 * The one exception that stops a run (INT-R08, standards S6.2). It is never caught to continue.
 * Its message never holds an account id, card number, amount or record image (standards S6.5).
 */
public class InterestRunStoppedException extends RuntimeException {

    private final StopReason reason;

    public InterestRunStoppedException(StopReason reason, String detail) {
        this(reason, detail, null);
    }

    public InterestRunStoppedException(StopReason reason, String detail, Throwable cause) {
        super(reason + " (" + reason.ruleId() + "): " + detail, cause);
        this.reason = reason;
    }

    public StopReason reason() {
        return reason;
    }
}
