package com.carddemo.interest.recon;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.jdbc.InterestTransactionReader;
import com.carddemo.interest.jdbc.ReplicaTable;
import com.carddemo.interest.jdbc.ReplicaTableReader;
import com.carddemo.interest.load.LoadFigures;

/** Reads the database side of the report. SELECT only, in one read-only REPEATABLE READ transaction. */
@Repository
public class ReconDatabase {

    private static final String RUN_COLUMNS = """
            run_id, status, parm_date,
            loaded_account_count, loaded_card_xref_count, loaded_tran_cat_balance_count, loaded_disclosure_group_count,
            loaded_acct_curr_bal_sum, loaded_acct_credit_limit_sum, loaded_acct_cash_credit_limit_sum,
            loaded_acct_curr_cyc_credit_sum, loaded_acct_curr_cyc_debit_sum, loaded_tran_cat_bal_sum, loaded_dis_int_rate_sum,
            loaded_acct_id_sum, loaded_xref_acct_id_sum, loaded_trancat_acct_id_sum,
            written_transaction_count, written_tran_amt_sum, written_account_count, written_acct_curr_bal_sum""";

    private final JdbcTemplate jdbc;
    private final ReplicaTableReader tables;
    private final InterestTransactionReader transactions;
    private final TransactionTemplate readOnly;

    public ReconDatabase(JdbcTemplate jdbc, ReplicaTableReader tables, InterestTransactionReader transactions,
            PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.tables = tables;
        this.transactions = transactions;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
        this.readOnly.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    /** The given run, or the newest one when none is given. */
    public DatabaseSide read(OptionalLong runId, CodePage codePage) {
        try {
            return readOnly.execute(status -> {
                List<RunRow> runs = runId.isPresent()
                        ? jdbc.query("SELECT " + RUN_COLUMNS + " FROM carddemo.interest_run WHERE run_id = ?",
                                (rs, n) -> runRow(rs), runId.getAsLong())
                        : jdbc.query("SELECT " + RUN_COLUMNS + " FROM carddemo.interest_run ORDER BY run_id DESC LIMIT 1",
                                (rs, n) -> runRow(rs));
                if (runs.isEmpty()) {
                    return new DatabaseSide(null, Map.of(), null, null, null);
                }
                RunRow run = runs.get(0);
                Map<ReplicaTable, List<byte[]>> records = new EnumMap<>(ReplicaTable.class);
                if (run.loadedRun()) {
                    for (ReplicaTable table : ReplicaTable.values()) {
                        records.put(table, tables.records(table, codePage));
                    }
                }
                return new DatabaseSide(run, records, transactions.count(run.runId()), transactions.amountSum(run.runId()), null);
            });
        } catch (RuntimeException e) {
            return DatabaseSide.unavailable("the database cannot be read: " + e.getMessage());
        }
    }

    private static RunRow runRow(ResultSet rs) throws SQLException {
        boolean loadRecorded = rs.getObject("loaded_account_count") != null && rs.getObject("loaded_card_xref_count") != null
                && rs.getObject("loaded_tran_cat_balance_count") != null && rs.getObject("loaded_disclosure_group_count") != null;
        LoadFigures loaded = !loadRecorded ? null : new LoadFigures(
                rs.getLong("loaded_account_count"), rs.getLong("loaded_card_xref_count"),
                rs.getLong("loaded_tran_cat_balance_count"), rs.getLong("loaded_disclosure_group_count"),
                rs.getBigDecimal("loaded_acct_curr_bal_sum"), rs.getBigDecimal("loaded_acct_credit_limit_sum"),
                rs.getBigDecimal("loaded_acct_cash_credit_limit_sum"), rs.getBigDecimal("loaded_acct_curr_cyc_credit_sum"),
                rs.getBigDecimal("loaded_acct_curr_cyc_debit_sum"), rs.getBigDecimal("loaded_tran_cat_bal_sum"),
                rs.getBigDecimal("loaded_dis_int_rate_sum"), rs.getBigDecimal("loaded_acct_id_sum"),
                rs.getBigDecimal("loaded_xref_acct_id_sum"), rs.getBigDecimal("loaded_trancat_acct_id_sum"));
        return new RunRow(rs.getLong("run_id"), rs.getString("status"), rs.getString("parm_date"), loaded,
                (Long) rs.getObject("written_transaction_count"), rs.getBigDecimal("written_tran_amt_sum"),
                (Long) rs.getObject("written_account_count"), rs.getBigDecimal("written_acct_curr_bal_sum"));
    }
}
