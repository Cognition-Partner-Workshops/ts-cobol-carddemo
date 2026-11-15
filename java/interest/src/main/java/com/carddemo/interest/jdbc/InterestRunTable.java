package com.carddemo.interest.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * carddemo.interest_run as the calculation uses it (interest-run-control, architecture.md
 * sections 2 and 3): the row of the run, the figures the calculation wrote (RC5) and the status.
 * Status values are those of the data map: LOADED, CALCULATED, EXPORTED, FAILED, ABANDONED.
 */
public final class InterestRunTable {

    private final JdbcTemplate jdbc;

    public InterestRunTable(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * A new row for a run whose replica tables are already loaded. The loader (ticket 05) creates
     * the row when it runs first; this is for a run that starts at the calculation.
     */
    public long create(String parmDate) {
        return jdbc.queryForObject("""
                INSERT INTO carddemo.interest_run (parm_date, started_at, status)
                VALUES (?, now(), 'LOADED')
                RETURNING run_id""", Long.class, parmDate);
    }

    /** RC5: what the calculation wrote, taken from the tables after the last commit. */
    public void calculated(long interestRunId, int accountsUpdated) {
        jdbc.update("""
                UPDATE carddemo.interest_run
                   SET status = 'CALCULATED',
                       written_transaction_count = (SELECT count(*) FROM carddemo.interest_transaction WHERE run_id = ?),
                       written_tran_amt_sum = (SELECT coalesce(sum(tran_amt), 0) FROM carddemo.interest_transaction WHERE run_id = ?),
                       written_account_count = ?,
                       written_acct_curr_bal_sum = (SELECT coalesce(sum(acct_curr_bal), 0) FROM carddemo.account)
                 WHERE run_id = ?""", interestRunId, interestRunId, accountsUpdated, interestRunId);
    }

    public void failed(long interestRunId) {
        jdbc.update("UPDATE carddemo.interest_run SET status = 'FAILED', finished_at = now() WHERE run_id = ?", interestRunId);
    }
}
