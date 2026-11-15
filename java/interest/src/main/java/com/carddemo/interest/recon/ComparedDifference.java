package com.carddemo.interest.recon;

/**
 * One difference as tools/parity/compare.py reports it: where (dataset, record and key, or the
 * SYSOUT file), the copybook field, the legacy value and the target value.
 */
public record ComparedDifference(String where, String field, String legacy, String target) {
}
