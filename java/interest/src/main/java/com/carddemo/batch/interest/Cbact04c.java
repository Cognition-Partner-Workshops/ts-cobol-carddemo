package com.carddemo.batch.interest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Java port of CBACT04C (INTCALC STEP15): monthly interest per transaction-category balance.
 *
 * <p>Paragraph names in comments refer to app/cbl/CBACT04C.cbl. Legacy behaviour is reproduced
 * as observed under GnuCOBOL, including:
 * <ul>
 *   <li>interest is {@code (TRAN-CAT-BAL * DIS-INT-RATE) / 1200} truncated toward zero to cents
 *       (no ROUNDED) and to the S9(9)V99 picture (high-order digits dropped, no ON SIZE ERROR);</li>
 *   <li>an account is rewritten only when the next account starts, so the last account on
 *       TCATBALF is never rewritten (the end-of-file update branch is unreachable);</li>
 *   <li>a zero rate writes no transaction but the account is still rewritten with both cycle
 *       amounts reset to zero;</li>
 *   <li>a missing disclosure group falls back to {@code DEFAULT}; if that is missing too, or the
 *       account or its card cross-reference is missing, the program abends with U999 / RC 12
 *       and keeps whatever it already wrote.</li>
 * </ul>
 */
final class Cbact04c {
    static final int ACCT_LRECL = 300;
    static final int XREF_LRECL = 50;
    static final int TCATBAL_LRECL = 50;
    static final int DISCGRP_LRECL = 50;
    static final int TRAN_LRECL = 350;

    private static final int ABEND_CODE = 999;
    private static final String DEFAULT_GROUP = "DEFAULT   ";
    private static final BigDecimal MONTHS_TIMES_PERCENT = BigDecimal.valueOf(1200);
    /** Files still open at an abend, in the order libcob closes them implicitly. */
    private static final String[] IMPLICIT_CLOSE = {
        "TRANSACT-FILE ('TRANSACT')", "ACCOUNT-FILE ('ACCTFILE')", "DISCGRP-FILE ('DISCGRP')",
        "XREF-FILE ('XREFFILE')", "TCATBAL-FILE ('TCATBALF')",
    };

    private final IndexedFile tcatbal;
    private final IndexedFile xref;
    private final IndexedFile discgrp;
    private final IndexedFile accounts;
    private final String parmDate;
    private final Supplier<String> db2Timestamp;

    private final List<String> sysout = new ArrayList<>();
    private final List<String> stderr = new ArrayList<>();
    private final List<String> transactions = new ArrayList<>();

    private String lastAcctNum = " ".repeat(11);
    private boolean firstTime = true;
    private BigDecimal totalInt = BigDecimal.ZERO;
    private int tranIdSuffix;
    private String accountRecord;
    private String xrefRecord;
    private String disGroupRecord;

    Cbact04c(IndexedFile tcatbal, IndexedFile xref, IndexedFile discgrp, IndexedFile accounts,
             String parm, Supplier<String> db2Timestamp) {
        this.tcatbal = tcatbal;
        this.xref = xref;
        this.discgrp = discgrp;
        this.accounts = accounts;
        this.parmDate = Text.pad(parm, 10);
        this.db2Timestamp = db2Timestamp;
    }

    /** Runs the program; returns the step return code (0, or 12 after an abend). */
    int run() {
        display("START OF EXECUTION OF PROGRAM CBACT04C");
        try {
            for (String tranCatBal : tcatbal.inKeyOrder()) {
                display(tranCatBal);
                String acctId = tranCatBal.substring(0, 11);
                if (!acctId.equals(lastAcctNum)) {
                    if (!firstTime) {
                        updateAccount();
                    } else {
                        firstTime = false;
                    }
                    totalInt = BigDecimal.ZERO;
                    lastAcctNum = acctId;
                    getAcctData(acctId);
                    getXrefData(acctId);
                }
                String typeCd = tranCatBal.substring(11, 13);
                String catCd = tranCatBal.substring(13, 17);
                getInterestRate(accountRecord.substring(112, 122) + typeCd + catCd, typeCd + catCd);
                BigDecimal rate = Zoned.decode(disGroupRecord.substring(16, 22), 2);
                if (rate.signum() != 0) {
                    computeInterest(Zoned.decode(tranCatBal.substring(17, 28), 2), rate);
                }
            }
        } catch (Abend abend) {
            for (String file : IMPLICIT_CLOSE) {
                stderr.add("libcob: warning: implicit CLOSE of " + file);
            }
            return 12;
        }
        display("END OF EXECUTION OF PROGRAM CBACT04C");
        return 0;
    }

    /** 1050-UPDATE-ACCOUNT. */
    private void updateAccount() {
        BigDecimal balance = Zoned.decode(accountRecord.substring(12, 24), 2).add(totalInt);
        String zero = Zoned.encode(BigDecimal.ZERO, 12, 2, true);
        accountRecord = accountRecord.substring(0, 12) + Zoned.encode(balance, 12, 2, true)
            + accountRecord.substring(24, 78) + zero + zero + accountRecord.substring(102);
        accounts.rewrite(accountRecord);
    }

    /** 1100-GET-ACCT-DATA. */
    private void getAcctData(String acctId) {
        String record = accounts.read(acctId);
        if (record == null) {
            display("ACCOUNT NOT FOUND: " + acctId);
            abend("ERROR READING ACCOUNT FILE", "23");
        }
        accountRecord = record;
    }

    /** 1110-GET-XREF-DATA (READ ... KEY IS FD-XREF-ACCT-ID through the alternate index). */
    private void getXrefData(String acctId) {
        String record = xref.readAlternate(acctId);
        if (record == null) {
            display("ACCOUNT NOT FOUND: " + acctId);
            abend("ERROR READING XREF FILE", "23");
        }
        xrefRecord = record;
    }

    /** 1200-GET-INTEREST-RATE and 1200-A-GET-DEFAULT-INT-RATE. */
    private void getInterestRate(String key, String typeAndCategory) {
        String record = discgrp.read(key);
        if (record != null) {
            disGroupRecord = record;
            return;
        }
        display("DISCLOSURE GROUP RECORD MISSING");
        display("TRY WITH DEFAULT GROUP CODE");
        record = discgrp.read(DEFAULT_GROUP + typeAndCategory);
        if (record == null) {
            abend("ERROR READING DEFAULT DISCLOSURE GROUP", "23");
        }
        disGroupRecord = record;
    }

    /** 1300-COMPUTE-INTEREST: COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200. */
    private void computeInterest(BigDecimal tranCatBal, BigDecimal rate) {
        BigDecimal monthlyInt = monthlyInterest(tranCatBal, rate);
        totalInt = Zoned.truncate(totalInt.add(monthlyInt), 11, 2);
        writeTransaction(monthlyInt);
    }

    /** WS-MONTHLY-INT as COBOL stores it: truncated to cents and to S9(9)V99. */
    static BigDecimal monthlyInterest(BigDecimal tranCatBal, BigDecimal rate) {
        return Zoned.truncate(tranCatBal.multiply(rate).divide(MONTHS_TIMES_PERCENT, 2, RoundingMode.DOWN), 11, 2);
    }

    /** 1300-B-WRITE-TX. */
    private void writeTransaction(BigDecimal monthlyInt) {
        tranIdSuffix = (tranIdSuffix + 1) % 1_000_000;
        // Z-GET-DB2-FORMAT-TIMESTAMP runs per transaction, once for both ORIG-TS and PROC-TS.
        String timestamp = db2Timestamp.get();
        String record = parmDate + String.format("%06d", tranIdSuffix)
            + "01"
            + "0005"
            + Text.pad("System", 10)
            + Text.pad("Int. for a/c " + accountRecord.substring(0, 11), 100)
            + Zoned.encode(monthlyInt, 11, 2, true)
            + "000000000"
            + Text.pad("", 50)
            + Text.pad("", 50)
            + Text.pad("", 10)
            + xrefRecord.substring(0, 16)
            + timestamp
            + timestamp
            + Text.pad("", 20);
        transactions.add(record);
    }

    /** 9910-DISPLAY-IO-STATUS, 9999-ABEND-PROGRAM and the CEE3ABD stub. */
    private void abend(String message, String fileStatus) {
        display(message);
        display("FILE STATUS IS: NNNN00" + fileStatus);
        display("ABENDING PROGRAM");
        display(String.format("CEE3ABD: USER ABEND U+%010d", ABEND_CODE));
        throw new Abend();
    }

    private void display(String line) {
        sysout.add(line);
    }

    List<String> sysout() {
        return sysout;
    }

    List<String> stderr() {
        return stderr;
    }

    List<String> transactions() {
        return transactions;
    }

    private static final class Abend extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Abend() {
            super("CBACT04C abend U999", null, false, false);
        }
    }
}
