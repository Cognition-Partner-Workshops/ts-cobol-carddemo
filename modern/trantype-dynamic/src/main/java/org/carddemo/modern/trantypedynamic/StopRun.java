package org.carddemo.modern.trantypedynamic;

/**
 * STOP RUN in a called program: the run unit ends there, control never returns to the caller, and
 * the step completes with the RETURN-CODE of the program that stopped it.
 */
final class StopRun extends RuntimeException {
    final int returnCode;

    StopRun(int returnCode) {
        super(null, null, false, false);
        this.returnCode = returnCode;
    }
}
