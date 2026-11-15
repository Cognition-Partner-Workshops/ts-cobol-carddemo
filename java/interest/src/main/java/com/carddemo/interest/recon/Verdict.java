package com.carddemo.interest.recon;

/** The result of one check. NOT_AVAILABLE is a figure that could not be computed; it is never counted as equal. */
public enum Verdict {
    EQUAL("EQUAL"),
    DIFFERENT("DIFFERENT"),
    NOT_AVAILABLE("NOT AVAILABLE");

    private final String text;

    Verdict(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
