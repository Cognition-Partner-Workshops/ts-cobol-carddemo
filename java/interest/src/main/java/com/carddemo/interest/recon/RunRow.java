package com.carddemo.interest.recon;

import java.math.BigDecimal;

import com.carddemo.interest.load.LoadFigures;

/** One row of {@code carddemo.interest_run}: what the load recorded (RC1, RC2) and what the calculation recorded (RC5). */
public record RunRow(long runId, String status, String parmDate, LoadFigures loaded,
        Long writtenTransactionCount, BigDecimal writtenTranAmtSum, Long writtenAccountCount, BigDecimal writtenAcctCurrBalSum) {

    /** The four tables hold this run's load. */
    public boolean loadedRun() {
        return "LOADED".equals(status) || calculated();
    }

    /** The calculation has updated the account table and written the run's transactions. */
    public boolean calculated() {
        return "CALCULATED".equals(status) || "EXPORTED".equals(status);
    }
}
