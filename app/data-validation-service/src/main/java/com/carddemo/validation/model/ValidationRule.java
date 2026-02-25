package com.carddemo.validation.model;

/**
 * Enumeration of supported validation rules.
 */
public enum ValidationRule {

    /** Compare row counts between source and target. */
    ROW_COUNT,

    /** Compare MD5/SHA checksums of all rows between source and target. */
    CHECKSUM,

    /** Compare a sample of individual records field-by-field. */
    SAMPLE_DIFF
}
