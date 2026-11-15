package com.carddemo.interest.batch;

/** The exit codes of the job, in one place (standards S6.2). */
public final class InterestExitCode {

    /** Normal end (INT-T03): RC 0. */
    public static final int COMPLETED = 0;

    /**
     * The run stopped (INT-R08). The value is REL-10, OPEN: U0999 on the host, RC 12 from the
     * off-host stub tools/stubs/CEE3ABD.cbl. 12 is used until operations decide.
     */
    public static final int STOPPED = 12;

    private InterestExitCode() {
    }
}
