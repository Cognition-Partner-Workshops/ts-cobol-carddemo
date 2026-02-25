package com.carddemo.validation.model;

/**
 * Overall status of a single validation check.
 */
public enum ValidationStatus {

    /** Validation passed – source and target match. */
    PASS,

    /** Validation failed – a mismatch was detected. */
    FAIL,

    /** Validation was skipped (e.g. source unavailable). */
    SKIPPED,

    /** An error occurred during validation. */
    ERROR
}
