package com.carddemo.interest.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;

import com.carddemo.interest.codec.CategoryBalanceRecord;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.FieldSpec;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.domain.AccountBalances.BalanceRead;
import com.carddemo.interest.domain.CategoryBalance;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.port.BalanceSource;

/**
 * FI-3 on carddemo.tran_cat_balance: the category balances in key order, one account at a time
 * (INT-R01, CBACT04C:28-32, 325-348). This is the one place the key order of the table is fixed
 * (standards S3.7, rule E6).
 *
 * <p>The rows are read in pages by key, each page after the last row of the page before, so no
 * cursor stays open across the commits of the step. One row is always read ahead: an account is
 * complete when the row ahead belongs to another account, and it ends the input (INT-Q04) when
 * there is no row ahead. No query is issued per account.
 *
 * <p>A restart reads the accounts after the last committed one (architecture.md section 6).
 */
public final class JdbcBalanceSource implements BalanceSource {

    public static final String TABLE = "carddemo.tran_cat_balance";
    public static final int DEFAULT_PAGE_ROWS = 1000;

    /**
     * Data-map rows CVTRA01Y/TRANCAT-ACCT-ID to CVTRA01Y/FILLER in copybook order, so column n is
     * field n of the layout. The row comparison and the ORDER BY use the column collations of FI-4;
     * COLLATE "C" on the type code is written out because the order must not depend on them (E6).
     */
    private static final String SELECT = """
            SELECT trancat_acct_id, trancat_type_cd, trancat_cd, tran_cat_bal, trancat_filler
              FROM carddemo.tran_cat_balance""";
    private static final String KEY_ORDER = """

             ORDER BY trancat_acct_id, trancat_type_cd COLLATE "C", trancat_cd
             LIMIT ?""";
    private static final String FIRST_PAGE = SELECT + KEY_ORDER;
    private static final String FIRST_PAGE_AFTER_ACCOUNT = SELECT + "\n WHERE trancat_acct_id > ?" + KEY_ORDER;
    private static final String NEXT_PAGE = SELECT
            + "\n WHERE (trancat_acct_id, trancat_type_cd COLLATE \"C\", trancat_cd) > (?, ? COLLATE \"C\", ?)" + KEY_ORDER;

    private record Row(CategoryBalance balance, String recordImage) {
    }

    private final JdbcOperations jdbc;
    private final CodePage codePage;
    private final CategoryBalanceRecord balanceRecord;
    private final int pageRows;
    private final Long afterAccountId;

    private final Deque<Row> page = new ArrayDeque<>();
    private CategoryBalance lastRowRead;
    private boolean tableExhausted;
    private int rowsRead;

    /** From the first account of the table. */
    public JdbcBalanceSource(JdbcOperations jdbc, CodePage codePage) {
        this(jdbc, codePage, null, DEFAULT_PAGE_ROWS);
    }

    /** From the account after {@code afterAccountId}; null reads from the first account. */
    public JdbcBalanceSource(JdbcOperations jdbc, CodePage codePage, Long afterAccountId, int pageRows) {
        if (pageRows < 1) {
            throw new IllegalArgumentException("pageRows must be at least 1");
        }
        this.jdbc = jdbc;
        this.codePage = codePage;
        this.balanceRecord = new CategoryBalanceRecord(codePage);
        this.afterAccountId = afterAccountId;
        this.pageRows = pageRows;
    }

    @Override
    public Optional<AccountBalances> nextAccount() {
        Row first = nextRow();
        if (first == null) {
            return Optional.empty();
        }
        long accountId = first.balance().accountId();
        List<BalanceRead> balances = new ArrayList<>();
        balances.add(new BalanceRead(first.balance(), first.recordImage()));
        for (Row ahead = rowAhead(); ahead != null && ahead.balance().accountId() == accountId; ahead = rowAhead()) {
            Row row = nextRow();
            balances.add(new BalanceRead(row.balance(), row.recordImage()));
        }
        return Optional.of(new AccountBalances(accountId, balances, rowAhead() == null));
    }

    private Row nextRow() {
        rowAhead();
        return page.poll();
    }

    /** The row after the ones handed out, or null at the end of the table. */
    private Row rowAhead() {
        if (page.isEmpty() && !tableExhausted) {
            List<Row> rows = readPage();
            page.addAll(rows);
            tableExhausted = rows.size() < pageRows;
            if (!rows.isEmpty()) {
                lastRowRead = rows.get(rows.size() - 1).balance();
            }
        }
        return page.peek();
    }

    private List<Row> readPage() {
        try {
            if (lastRowRead != null) {
                return jdbc.query(NEXT_PAGE, this::row, lastRowRead.accountId(), lastRowRead.typeCode(),
                        lastRowRead.categoryCode(), pageRows);
            }
            if (afterAccountId != null) {
                return jdbc.query(FIRST_PAGE_AFTER_ACCOUNT, this::row, afterAccountId, pageRows);
            }
            return jdbc.query(FIRST_PAGE, this::row, pageRows);
        } catch (DataAccessException e) {
            throw new InterestRunStoppedException(StopReason.OPEN_INPUT, "cannot read table " + TABLE, e);
        }
    }

    /**
     * The row as its 50-byte record: the image is what the run log echoes (CBACT04C:193, standards
     * S6.5), and decoding it applies rules E1 to E3 to the row exactly as to a record of the file.
     */
    private Row row(ResultSet resultSet, int rowNumber) throws SQLException {
        RecordSource source = new RecordSource(TABLE, ++rowsRead);
        RecordValues values = RowValues.of(CategoryBalanceRecord.LAYOUT, resultSet);
        byte[] record = balanceRecord.codec().encode(values, source);
        return new Row(balanceRecord.decode(record, source), codePage.display(record));
    }

    /** Column n of a row is field n of the layout (the tables of FI-4 keep the copybook order). */
    static final class RowValues {

        private RowValues() {
        }

        static RecordValues of(RecordLayout layout, ResultSet row) throws SQLException {
            RecordValues values = RecordValues.of(layout);
            for (FieldSpec field : layout.fields()) {
                int column = field.index() + 1;
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
}
