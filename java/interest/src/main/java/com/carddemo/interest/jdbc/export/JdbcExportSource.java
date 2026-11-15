package com.carddemo.interest.jdbc.export;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.export.ExportSource;

/**
 * The export's access to schema carddemo (FI-4, db/postgres/02-interest.sql). It reads
 * interest_run, interest_transaction and account and changes one thing: the status of the run,
 * from CALCULATED to EXPORTED. Column names are here and nowhere else in the export; the key
 * orders of the two datasets are fixed by the two ORDER BY clauses (standards S3.7).
 */
public final class JdbcExportSource implements ExportSource {

    private static final String EXPORTED = "EXPORTED";

    private final JdbcTemplate jdbc;

    public JdbcExportSource(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<RunToExport> run(long runId) {
        return jdbc.query("""
                SELECT run_id, status, written_transaction_count, written_tran_amt_sum
                  FROM carddemo.interest_run
                 WHERE run_id = ?
                """,
                (row, n) -> new RunToExport(row.getLong("run_id"), row.getString("status"),
                        row.getObject("written_transaction_count", Long.class),
                        row.getBigDecimal("written_tran_amt_sum")),
                runId).stream().findFirst();
    }

    @Override
    public List<TransactionRow> transactions(long runId) {
        return jdbc.query("""
                SELECT seq_no, tran_id, tran_type_cd, tran_cat_cd, tran_source, tran_desc, tran_amt,
                       tran_merchant_id, tran_merchant_name, tran_merchant_city, tran_merchant_zip,
                       tran_card_num, tran_orig_ts, tran_proc_ts, tran_filler
                  FROM carddemo.interest_transaction
                 WHERE run_id = ?
                 ORDER BY seq_no
                """,
                (row, n) -> new TransactionRow(row.getInt("seq_no"), row.getString("tran_id"),
                        row.getString("tran_type_cd"), row.getLong("tran_cat_cd"), row.getString("tran_source"),
                        row.getString("tran_desc"), row.getBigDecimal("tran_amt"), row.getLong("tran_merchant_id"),
                        row.getString("tran_merchant_name"), row.getString("tran_merchant_city"),
                        row.getString("tran_merchant_zip"), row.getString("tran_card_num"),
                        row.getString("tran_orig_ts"), row.getString("tran_proc_ts"), row.getBytes("tran_filler")),
                runId);
    }

    @Override
    public List<AccountRow> accounts() {
        return jdbc.query("""
                SELECT acct_id, acct_active_status, acct_curr_bal, acct_credit_limit, acct_cash_credit_limit,
                       acct_open_date, acct_expiraion_date, acct_reissue_date, acct_curr_cyc_credit,
                       acct_curr_cyc_debit, acct_addr_zip, acct_group_id, acct_filler
                  FROM carddemo.account
                 ORDER BY acct_id
                """,
                (row, n) -> new AccountRow(row.getLong("acct_id"), row.getString("acct_active_status"),
                        row.getBigDecimal("acct_curr_bal"), row.getBigDecimal("acct_credit_limit"),
                        row.getBigDecimal("acct_cash_credit_limit"), row.getString("acct_open_date"),
                        row.getString("acct_expiraion_date"), row.getString("acct_reissue_date"),
                        row.getBigDecimal("acct_curr_cyc_credit"), row.getBigDecimal("acct_curr_cyc_debit"),
                        row.getString("acct_addr_zip"), row.getString("acct_group_id"),
                        row.getBytes("acct_filler")));
    }

    @Override
    public boolean markExported(long runId) {
        return jdbc.update("UPDATE carddemo.interest_run SET status = ? WHERE run_id = ? AND status = ?",
                EXPORTED, runId, CALCULATED) == 1;
    }
}
