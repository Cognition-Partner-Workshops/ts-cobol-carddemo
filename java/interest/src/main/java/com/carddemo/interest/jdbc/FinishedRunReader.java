package com.carddemo.interest.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.carddemo.interest.codec.AccountRecord;
import com.carddemo.interest.codec.FieldSpec;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.codec.TransactionRecord;

/**
 * Reads what a finished run left in the tables, for the export (architecture.md section 2,
 * interest-export). It only reads: no statement here changes a row.
 *
 * <p>This is the row mapping of the two exported layouts. The columns of each query are listed in
 * the order of the fields of the copybook, so the n-th column is the n-th field of the layout
 * (data-map rows CVTRA05Y/* and CVACT01Y/*); no copybook field name is repeated here (standards S2.2).
 */
@Repository
public class FinishedRunReader {

    public static final String TRANSACTION_TABLE = "carddemo.interest_transaction";
    public static final String ACCOUNT_TABLE = "carddemo.account";

    /**
     * Data-map rows CVTRA05Y/TRAN-ID to CVTRA05Y/TRAN-PROC-TS, in copybook order. The FILLER is not
     * read: CBACT04C never sets it (INT-T04) and rule E4 writes it as spaces of the output code page (G05).
     */
    private static final String TRANSACTIONS_IN_WRITE_ORDER = """
            SELECT tran_id, tran_type_cd, tran_cat_cd, tran_source, tran_desc, tran_amt, tran_merchant_id,
                   tran_merchant_name, tran_merchant_city, tran_merchant_zip, tran_card_num, tran_orig_ts, tran_proc_ts
              FROM carddemo.interest_transaction
             WHERE run_id = ?
             ORDER BY seq_no""";

    /** Data-map rows CVACT01Y/ACCT-ID to CVACT01Y/FILLER, in copybook order; key order of the KSDS (ACCTFILE.jcl:36). */
    private static final String ACCOUNTS_IN_KEY_ORDER = """
            SELECT acct_id, acct_active_status, acct_curr_bal, acct_credit_limit, acct_cash_credit_limit,
                   acct_open_date, acct_expiraion_date, acct_reissue_date, acct_curr_cyc_credit, acct_curr_cyc_debit,
                   acct_addr_zip, acct_group_id, acct_filler
              FROM carddemo.account
             ORDER BY acct_id""";

    private static final String RUN_STATUS = "SELECT status FROM carddemo.interest_run WHERE run_id = ?";

    private static final int FETCH_SIZE = 1000;

    private final JdbcTemplate jdbc;

    public FinishedRunReader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The status of the run in interest_run, or empty when there is no such run. */
    public Optional<String> statusOf(long runId) {
        List<String> status = jdbc.queryForList(RUN_STATUS, String.class, runId);
        return status.stream().findFirst();
    }

    /** The interest transactions of the run in the order they were written (seq_no): one SYSTRAN generation. */
    public void transactionsInWriteOrder(long runId, Consumer<RecordValues> eachTransaction) {
        jdbc.query(connection -> {
            var statement = connection.prepareStatement(TRANSACTIONS_IN_WRITE_ORDER);
            statement.setFetchSize(FETCH_SIZE);
            statement.setLong(1, runId);
            return statement;
        }, row -> {
            eachTransaction.accept(valuesOf(TransactionRecord.LAYOUT, row));
        });
    }

    /** Every account in key order (acct_id): the ACCTDATA image. */
    public void accountsInKeyOrder(Consumer<RecordValues> eachAccount) {
        jdbc.query(connection -> {
            var statement = connection.prepareStatement(ACCOUNTS_IN_KEY_ORDER);
            statement.setFetchSize(FETCH_SIZE);
            return statement;
        }, row -> {
            eachAccount.accept(valuesOf(AccountRecord.LAYOUT, row));
        });
    }

    /** Column n of the row is field n of the layout; a FILLER with no column stays unset. */
    private static RecordValues valuesOf(RecordLayout layout, ResultSet row) throws SQLException {
        int columns = row.getMetaData().getColumnCount();
        RecordValues values = RecordValues.of(layout);
        for (FieldSpec field : layout.fields()) {
            int column = field.index() + 1;
            if (column > columns) {
                continue;
            }
            values = switch (field.kind()) {
                case UNSIGNED_ZONED -> values.withUnsigned(field, row.getBigDecimal(column).longValueExact());
                case SIGNED_ZONED -> values.withSigned(field, row.getBigDecimal(column));
                case TEXT -> values.withText(field, row.getString(column));
                case FILLER -> values.withFiller(field, row.getBytes(column));
                case NOT_DISPLAY -> values;
            };
        }
        return values;
    }
}
