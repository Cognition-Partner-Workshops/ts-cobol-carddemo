package com.carddemo.interest.export;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The export's part of {@code carddemo.interest_run}: the only table the export writes. */
@Repository
class ExportRunJournal {

    static final String CALCULATED = "CALCULATED";
    static final String EXPORTED = "EXPORTED";

    /** The RC5 columns of a run; a figure the calculation did not record is null. */
    record Run(long runId, String status, Long transactionCount, BigDecimal tranAmtSum, Long accountCount, BigDecimal acctCurrBalSum) {
    }

    private final JdbcTemplate jdbc;

    ExportRunJournal(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Optional<Run> find(long runId) {
        List<Run> runs = jdbc.query("""
                SELECT run_id, status, written_transaction_count, written_tran_amt_sum, written_account_count, written_acct_curr_bal_sum
                  FROM carddemo.interest_run WHERE run_id = ?""",
                (rs, rowNumber) -> new Run(rs.getLong(1), rs.getString(2), (Long) rs.getObject(3), rs.getBigDecimal(4),
                        (Long) rs.getObject(5), rs.getBigDecimal(6)), runId);
        return runs.stream().findFirst();
    }

    /** The newest run that is calculated and not yet exported. */
    Optional<Long> newestCalculated() {
        return jdbc.queryForList("SELECT run_id FROM carddemo.interest_run WHERE status = ? ORDER BY run_id DESC LIMIT 1",
                Long.class, CALCULATED).stream().findFirst();
    }

    /**
     * CALCULATED to EXPORTED. A RC5 figure the calculation left empty takes the exported value; one it
     * recorded is not changed. False when the run is no longer CALCULATED.
     */
    boolean exported(ExportResult result, Instant finishedAt) {
        return jdbc.update("""
                UPDATE carddemo.interest_run
                   SET status = ?, finished_at = ?,
                       written_transaction_count = COALESCE(written_transaction_count, ?),
                       written_tran_amt_sum = COALESCE(written_tran_amt_sum, ?),
                       written_acct_curr_bal_sum = COALESCE(written_acct_curr_bal_sum, ?)
                 WHERE run_id = ? AND status = ?""",
                EXPORTED, Timestamp.from(finishedAt), result.transactionCount(), result.tranAmtSum(), result.acctCurrBalSum(),
                result.runId(), CALCULATED) == 1;
    }
}
