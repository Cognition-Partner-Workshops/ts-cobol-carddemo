package com.carddemo.interest.recon;

import java.util.Collection;

/** The result of one line, of one check or of a report. */
public enum Verdict {
    EQUAL("EQUAL"),
    DIFFERENT("DIFFERENT"),
    NOT_AVAILABLE("NOT AVAILABLE");

    private final String label;

    Verdict(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** DIFFERENT wins over NOT AVAILABLE, which wins over EQUAL; nothing checked is NOT AVAILABLE. */
    public static Verdict of(Collection<Verdict> verdicts) {
        if (verdicts.contains(DIFFERENT)) {
            return DIFFERENT;
        }
        return verdicts.isEmpty() || verdicts.contains(NOT_AVAILABLE) ? NOT_AVAILABLE : EQUAL;
    }
}
