package com.carddemo.interest.batch;

/** 9999-ABEND-PROGRAM: CALL 'CEE3ABD' with abend code 999 after the I/O error has been displayed. */
public class Abend extends RuntimeException {

    public static final int ABEND_CODE = 999;
    /** Step return code of the off-host CEE3ABD stand-in (tools/stubs/CEE3ABD.cbl). */
    public static final int RETURN_CODE = 12;

    public Abend(String message) {
        super(message);
    }

    /** DISPLAY of the error, 9910-DISPLAY-IO-STATUS for a numeric status such as "23", then the abend. */
    public static Abend afterIoError(Sysout sysout, String message, String fileStatus) {
        sysout.display(message);
        sysout.display("FILE STATUS IS: NNNN00" + fileStatus);
        sysout.display("ABENDING PROGRAM");
        sysout.display(String.format("CEE3ABD: USER ABEND U+%010d", ABEND_CODE));
        sysout.abend();
        return new Abend(message + " (file status " + fileStatus + ")");
    }
}
