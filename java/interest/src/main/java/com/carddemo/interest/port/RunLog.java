package com.carddemo.interest.port;

/**
 * FI-3. The run log is the legacy SYSOUT, byte for byte, and a parity output (INT-T02, standards S6.5).
 * It is not the application log.
 */
public interface RunLog {

    /** A fixed line the legacy displays. */
    void line(String text);

    /** A record as read, displayed as its image (CBACT04C:193). */
    void echo(String recordImage);
}
