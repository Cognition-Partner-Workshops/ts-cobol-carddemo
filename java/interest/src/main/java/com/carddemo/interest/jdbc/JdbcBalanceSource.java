package com.carddemo.interest.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import com.carddemo.interest.codec.CategoryBalanceRecord;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.domain.AccountBalances.BalanceRead;
import com.carddemo.interest.domain.CategoryBalance;
import com.carddemo.interest.port.BalanceSource;

/**
 * FI-3 on carddemo.tran_cat_balance: the balances in key order, one account at a time (INT-R01).
 * This query is the one place the key order is fixed (standards S3.7, rule E6). One forward-only
 * cursor on a connection of its own serves the whole step; the row after an account's last balance
 * is read ahead, so the account that ends the input is known without a second query (INT-Q04).
 */
public final class JdbcBalanceSource implements BalanceSource, AutoCloseable {

    private static final String BALANCES_IN_KEY_ORDER = """
            SELECT trancat_acct_id, trancat_type_cd, trancat_cd, tran_cat_bal, trancat_filler
              FROM carddemo.tran_cat_balance
             WHERE trancat_acct_id > ?
             ORDER BY trancat_acct_id, trancat_type_cd COLLATE "C", trancat_cd""";
    private static final int FETCH_SIZE = 1000;
    private static final RecordLayout LAYOUT = CategoryBalanceRecord.LAYOUT;
    private static final RecordSource TABLE = RecordSource.built("carddemo.tran_cat_balance");

    private final Connection connection;
    private final PreparedStatement statement;
    private final ResultSet rows;
    /** The row read ahead; null when the input has ended. */
    private BalanceRead ahead;

    private JdbcBalanceSource(Connection connection, PreparedStatement statement, ResultSet rows) throws SQLException {
        this.connection = connection;
        this.statement = statement;
        this.rows = rows;
        this.ahead = nextRow();
    }

    /**
     * Opens the cursor after the given account: -1 for a new run, the last committed account for a
     * restart (architecture.md section 6).
     */
    public static JdbcBalanceSource openAfter(DataSource dataSource, long lastCommittedAccountId) {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            // PostgreSQL streams a result set only inside a transaction; this one never writes
            connection.setAutoCommit(false);
            connection.setReadOnly(true);
            PreparedStatement statement = connection.prepareStatement(BALANCES_IN_KEY_ORDER);
            statement.setFetchSize(FETCH_SIZE);
            statement.setBigDecimal(1, BigDecimal.valueOf(lastCommittedAccountId));
            return new JdbcBalanceSource(connection, statement, statement.executeQuery());
        } catch (SQLException e) {
            closeQuietly(connection);
            throw new JdbcAccessFailure("cannot read carddemo.tran_cat_balance", e);
        }
    }

    @Override
    public Optional<AccountBalances> nextAccount() {
        if (ahead == null) {
            return Optional.empty();
        }
        long accountId = ahead.balance().accountId();
        List<BalanceRead> balances = new ArrayList<>();
        try {
            while (ahead != null && ahead.balance().accountId() == accountId) {
                balances.add(ahead);
                ahead = nextRow();
            }
        } catch (SQLException e) {
            throw new JdbcAccessFailure("cannot read carddemo.tran_cat_balance", e);
        }
        return Optional.of(new AccountBalances(accountId, balances, ahead == null));
    }

    private BalanceRead nextRow() throws SQLException {
        if (!rows.next()) {
            return null;
        }
        CategoryBalance balance = new CategoryBalance(
                rows.getBigDecimal("trancat_acct_id").longValueExact(),
                rows.getString("trancat_type_cd").stripTrailing(),
                rows.getBigDecimal("trancat_cd").intValueExact(),
                rows.getBigDecimal("tran_cat_bal"));
        return new BalanceRead(balance, recordImage(balance, rows.getString("trancat_type_cd"), rows.getBytes("trancat_filler")));
    }

    /** The 50-byte record the legacy displays (CBACT04C:193, INT-T02): the row encoded by the codec, FILLER as stored. */
    private static String recordImage(CategoryBalance balance, String typeCode, byte[] filler) {
        byte[] record = LAYOUT.encode(LAYOUT.blank(CodePage.FIXTURE)
                .with("TRANCAT-ACCT-ID", BigDecimal.valueOf(balance.accountId()))
                .with("TRANCAT-TYPE-CD", typeCode)
                .with("TRANCAT-CD", BigDecimal.valueOf(balance.categoryCode()))
                .with("TRAN-CAT-BAL", balance.balance()), CodePage.FIXTURE, TABLE);
        return RecordImage.image(LAYOUT.replace(record, "FILLER", filler, CodePage.FIXTURE, TABLE));
    }

    @Override
    public void close() {
        try {
            rows.close();
            statement.close();
        } catch (SQLException e) {
            throw new JdbcAccessFailure("cannot close the cursor on carddemo.tran_cat_balance", e);
        } finally {
            closeQuietly(connection);
        }
    }

    private static void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
            connection.setReadOnly(false);
            connection.setAutoCommit(true);
            connection.close();
        } catch (SQLException e) {
            // the connection goes back to the pool, which discards a broken one
        }
    }
}
