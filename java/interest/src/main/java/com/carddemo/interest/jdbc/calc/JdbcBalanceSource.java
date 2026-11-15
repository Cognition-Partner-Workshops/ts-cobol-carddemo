package com.carddemo.interest.jdbc.calc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.codec.CategoryBalanceRecord;
import com.carddemo.interest.codec.FixtureCodePage;
import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordFormat;
import com.carddemo.interest.codec.RecordLayout.FieldLayout;
import com.carddemo.interest.codec.RecordLayout.Kind;
import com.carddemo.interest.codec.RecordOrigin;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.domain.AccountBalances.BalanceRead;
import com.carddemo.interest.port.BalanceSource;

/**
 * The category balances of carddemo.tran_cat_balance in key order, one account at a time (INT-R01,
 * CBACT04C:28-32, 325-348; data-map rows CVTRA01Y/*, rule E6). The rows are read in pages that
 * continue after the last key read; one row is always read ahead, so the account that ends the
 * input (INT-Q04) is known without a query per account.
 */
public final class JdbcBalanceSource implements BalanceSource {

    static final String TABLE = "carddemo.tran_cat_balance";

    private static final String SELECT = "SELECT trancat_acct_id, trancat_type_cd, trancat_cd, tran_cat_bal, trancat_filler"
            + " FROM " + TABLE;
    /** The key order of TCATBALF, fixed here and nowhere else (standards S3.7, E6). */
    private static final String KEY_ORDER = " ORDER BY trancat_acct_id, trancat_type_cd COLLATE \"C\", trancat_cd LIMIT ?";
    private static final String FROM_THE_START = SELECT + KEY_ORDER;
    private static final String AFTER_ACCOUNT = SELECT + " WHERE trancat_acct_id > ?" + KEY_ORDER;
    private static final String AFTER_KEY = SELECT
            + " WHERE (trancat_acct_id, trancat_type_cd COLLATE \"C\", trancat_cd) > (?, CAST(? AS CHAR(2)), ?)" + KEY_ORDER;

    /** The record echoed to the run log is the fixture image of the row (INT-T02, standards S6.5). */
    private static final RecordCodec CODEC = RecordFormat.fixture().codec(CategoryBalanceRecord.LAYOUT);
    private static final FieldLayout FILLER = CategoryBalanceRecord.LAYOUT.fields().stream()
            .filter(field -> field.kind() == Kind.FILLER).findFirst().orElseThrow();

    private record Row(RecordValues values, byte[] filler) {

        long accountId() {
            return values.unsigned("TRANCAT-ACCT-ID");
        }
    }

    private final JdbcTemplate jdbc;
    private final int pageRows;
    private final OptionalLong afterAccount;
    private final Deque<Row> page = new ArrayDeque<>();
    private Row lastRead;
    private boolean inputEnded;
    private int rowsRead;

    /**
     * @param afterAccount the last account a committed chunk holds; empty for a run from the start
     *                     (architecture.md section 6)
     */
    public JdbcBalanceSource(JdbcTemplate jdbc, int pageRows, OptionalLong afterAccount) {
        if (pageRows < 1) {
            throw new IllegalArgumentException("a page holds at least one row");
        }
        this.jdbc = jdbc;
        this.pageRows = pageRows;
        this.afterAccount = afterAccount;
    }

    @Override
    public Optional<AccountBalances> nextAccount() {
        Row first = next();
        if (first == null) {
            return Optional.empty();
        }
        List<BalanceRead> balances = new ArrayList<>();
        balances.add(read(first));
        while (ahead() != null && ahead().accountId() == first.accountId()) {
            balances.add(read(next()));
        }
        return Optional.of(new AccountBalances(first.accountId(), balances, ahead() == null));
    }

    /** The next row without taking it; null at the end of the input. */
    private Row ahead() {
        if (page.isEmpty() && !inputEnded) {
            List<Row> rows = nextPage();
            inputEnded = rows.size() < pageRows;
            page.addAll(rows);
        }
        return page.peek();
    }

    private Row next() {
        Row row = ahead();
        if (row != null) {
            lastRead = page.poll();
        }
        return row;
    }

    private List<Row> nextPage() {
        if (lastRead != null) {
            return jdbc.query(AFTER_KEY, JdbcBalanceSource::row, lastRead.accountId(),
                    lastRead.values().text("TRANCAT-TYPE-CD"), lastRead.values().unsigned("TRANCAT-CD"), pageRows);
        }
        if (afterAccount.isPresent()) {
            return jdbc.query(AFTER_ACCOUNT, JdbcBalanceSource::row, afterAccount.getAsLong(), pageRows);
        }
        return jdbc.query(FROM_THE_START, JdbcBalanceSource::row, pageRows);
    }

    /** Data-map rows CVTRA01Y/TRANCAT-ACCT-ID, TRANCAT-TYPE-CD, TRANCAT-CD, TRAN-CAT-BAL, FILLER. */
    private static Row row(ResultSet rs, int rowNumber) throws SQLException {
        RecordValues values = RecordValues.blank(CategoryBalanceRecord.LAYOUT)
                .with("TRANCAT-ACCT-ID", rs.getLong("trancat_acct_id"))
                .with("TRANCAT-TYPE-CD", rs.getString("trancat_type_cd"))
                .with("TRANCAT-CD", rs.getLong("trancat_cd"))
                .with("TRAN-CAT-BAL", rs.getBigDecimal("tran_cat_bal"));
        return new Row(values, rs.getBytes("trancat_filler"));
    }

    private BalanceRead read(Row row) {
        rowsRead++;
        byte[] record = CODEC.encode(row.values(), new RecordOrigin(TABLE, rowsRead));
        // E4: the FILLER of the row goes back into the image as the bytes it holds
        System.arraycopy(row.filler(), 0, record, FILLER.offset(), FILLER.length());
        return new BalanceRead(CategoryBalanceRecord.from(row.values()), FixtureCodePage.image(record));
    }
}
