package com.carddemo.posting;

/** 9999-ABEND-PROGRAM: the legacy program calls CEE3ABD and the job step ends abnormally. */
public class LegacyAbend extends RuntimeException {

    public LegacyAbend(String message) {
        super(message);
    }
}
