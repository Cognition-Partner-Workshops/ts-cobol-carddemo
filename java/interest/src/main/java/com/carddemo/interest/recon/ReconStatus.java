package com.carddemo.interest.recon;

/** What one reconciliation line says about its two figures. */
public enum ReconStatus {

    EQUAL("EQUAL"),
    DIFFERENT("DIFFERENT"),
    /** One of the figures could not be computed. It is never shown as equal (ticket 10, Errors). */
    NOT_AVAILABLE("NOT AVAILABLE");

    private final String label;

    ReconStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
