package com.carddemo.interest.jdbc.calc;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * What calcInterestStep writes to carddemo.interest_run (architecture.md sections 2 and 3): the
 * row of a run that no earlier step opened, and the status and written figures of the calculation
 * (strategy RC5). No copybook; no account data.
 */
public final class CalcRunControl {

    static final String TABLE = "carddemo.interest_run";

    /** Statuses of interest_run.status the calculation sets (data map, carddemo.interest_run). */
    static final String LOADED = "LOADED";
    static final String CALCULATED = "CALCULATED";
    static final String FAILED = "FAILED";

    private static final String OPEN = "INSERT INTO " + TABLE + " (parm_date, started_at, status)"
            + " VALUES (?, now(), '" + LOADED + "') RETURNING run_id";
    private static final String FIGURES = "UPDATE " + TABLE + " r SET status = '" + CALCULATED + "',"
            + " written_account_count = ?,"
            + " written_transaction_count = (SELECT count(*) FROM " + JdbcTransactionSink.TABLE + " t WHERE t.run_id = r.run_id),"
            + " written_tran_amt_sum = (SELECT coalesce(sum(t.tran_amt), 0) FROM " + JdbcTransactionSink.TABLE
            + " t WHERE t.run_id = r.run_id),"
            + " written_acct_curr_bal_sum = (SELECT coalesce(sum(a.acct_curr_bal), 0) FROM " + JdbcAccountStore.TABLE + " a)"
            + " WHERE r.run_id = ?";
    private static final String STATUS = "UPDATE " + TABLE + " SET status = ? WHERE run_id = ?";

    private final JdbcTemplate jdbc;

    public CalcRunControl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Opens the row of a run whose tables were loaded outside the job; parm_date is the PARM text (INT-Q02). */
    public long open(String parmDate) {
        return jdbc.queryForObject(OPEN, Long.class, parmDate);
    }

    public void calculated(long interestRunId, long accountsUpdated) {
        jdbc.update(FIGURES, accountsUpdated, interestRunId);
    }

    public void failed(long interestRunId) {
        jdbc.update(STATUS, FAILED, interestRunId);
    }
}
