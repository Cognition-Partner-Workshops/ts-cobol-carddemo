package com.carddemo.modern.posting;

import java.util.List;

/** 9910-DISPLAY-IO-STATUS, 9999-ABEND-PROGRAM and the CEE3ABD STOP RUN stand-in. */
final class Abend {
    static final class Failure extends RuntimeException {
        final int status;
        Failure(String message, int status) { super(message); this.status = status; }
    }
    static final String[] POST_FILES = {"TCATBAL-FILE ('TCATBALF')", "ACCOUNT-FILE ('ACCTFILE')",
            "DALYREJS-FILE ('DALYREJS')", "XREF-FILE ('XREFFILE')", "TRANSACT-FILE ('TRANFILE')", "DALYTRAN-FILE ('DALYTRAN')"};
    static final String[] REPORT_FILES = {"DATE-PARMS-FILE ('DATEPARM')", "TRANCATG-FILE ('TRANCATG')",
            "TRANTYPE-FILE ('TRANTYPE')", "XREF-FILE ('CARDXREF')", "REPORT-FILE ('TRANREPT')", "TRANSACT-FILE ('TRANFILE')"};
    private Abend() { }
    static void append(List<String> out, String message, int status, String[] opened) {
        out.add(message);
        out.add(String.format(java.util.Locale.ROOT,"FILE STATUS IS: NNNN%04d",status));
        out.add("ABENDING PROGRAM");
        out.add("CEE3ABD: USER ABEND U+0000000999");
        out.add("");
        for (String file : opened) out.add("libcob: warning: implicit CLOSE of " + file);
    }

    static String[] reverse(List<String> opened) { return opened.reversed().toArray(String[]::new); }

    static void stopStep() {
        var context = org.springframework.batch.core.scope.context.StepSynchronizationManager.getContext();
        if (context != null) context.getStepExecution().setTerminateOnly();
    }
}
