package com.carddemo.interest.jdbc;

import java.math.BigDecimal;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;

/**
 * The calculation step's part of carddemo.interest_run (architecture.md sections 2 and 3): the
 * status of the run while and after it calculates, and the written figures of strategy item RC5.
 * started_at and finished_at are the wall clock of the database, not the run clock (data map).
 */
public final class JdbcInterestRuns {

    public static final String TABLE = "carddemo.interest_run";
    public static final String CALCULATING = "CALCULATING";
    public static final String CALCULATED = "CALCULATED";
    public static final String FAILED = "FAILED";

    private static final String START = """
            INSERT INTO carddemo.interest_run (parm_date, started_at, status)
            VALUES (?, clock_timestamp(), ?)
            RETURNING run_id""";
    private static final String SET_STATUS = "UPDATE carddemo.interest_run SET status = ? WHERE run_id = ?";
    private static final String FAIL =
            "UPDATE carddemo.interest_run SET status = ?, finished_at = clock_timestamp() WHERE run_id = ?";

    /**
     * RC5: the interest transactions the run inserted and their sum, read back from the table, and
     * the interest of the account that ends the input and is not added to it (INT-Q04).
     */
    private static final String CALCULATED_FIGURES = """
            UPDATE carddemo.interest_run r
               SET status = ?,
                   written_transaction_count = t.transactions,
                   written_tran_amt_sum = t.amount,
                   written_interest_not_added_sum = ?
              FROM (SELECT count(*) AS transactions, coalesce(sum(tran_amt), 0) AS amount
                      FROM carddemo.interest_transaction
                     WHERE run_id = ?) t
             WHERE r.run_id = ?""";

    private final JdbcOperations jdbc;

    public JdbcInterestRuns(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    /** A new run whose tables were not loaded by a step of this job; returns its run_id. */
    public long start(String parmDate) {
        try {
            return jdbc.queryForObject(START, Long.class, parmDate, CALCULATING);
        } catch (DataAccessException e) {
            throw new InterestRunStoppedException(StopReason.OPEN_INPUT, "cannot insert into table " + TABLE, e);
        }
    }

    /** The calculation of a run that exists - loaded by the step before, or being restarted - begins. */
    public void calculating(long runId) {
        setStatus(runId, CALCULATING, SET_STATUS);
    }

    public void calculated(long runId, BigDecimal interestNotAdded) {
        try {
            requireOneRow(jdbc.update(CALCULATED_FIGURES, CALCULATED, interestNotAdded, runId, runId));
        } catch (DataAccessException e) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot update table " + TABLE, e);
        }
    }

    public void failed(long runId) {
        setStatus(runId, FAILED, FAIL);
    }

    private void setStatus(long runId, String status, String sql) {
        try {
            requireOneRow(jdbc.update(sql, status, runId));
        } catch (DataAccessException e) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot update table " + TABLE, e);
        }
    }

    private static void requireOneRow(int updated) {
        if (updated != 1) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "the run is not in " + TABLE);
        }
    }
}
