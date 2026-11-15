package com.carddemo.interest.file;

/** Lets tests of other packages write rc.json with the main code's writer (FI-2). */
public final class RecordedReturnCodesForTests {

    private RecordedReturnCodesForTests() {
    }

    public static String json(int step15ReturnCode) {
        return RecordedReturnCodes.json(step15ReturnCode);
    }
}
