package com.carddemo.modern.interest;

/** 9999-ABEND-PROGRAM: CALL 'CEE3ABD' (the local stand-in ends the step with RC 12). */
public class CobolAbend extends RuntimeException {

    public static final int RETURN_CODE = 12;

    public CobolAbend(String reason) {
        super(reason);
    }
}
