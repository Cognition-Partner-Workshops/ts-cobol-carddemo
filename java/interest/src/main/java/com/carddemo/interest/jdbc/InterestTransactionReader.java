package com.carddemo.interest.jdbc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.carddemo.interest.codec.CardDemoLayouts;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.FieldKind;
import com.carddemo.interest.codec.LayoutField;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;

/**
 * Reads the interest transactions of one run from {@code carddemo.interest_transaction} as CVTRA05Y
 * records, in the order they were written (seq_no). One run is one SYSTRAN generation. Reads only.
 */
@Repository
public class InterestTransactionReader {

    private static final String TABLE = "carddemo.interest_transaction";
    private static final String FILLER_COLUMN = "tran_filler";
    private static final RecordLayout LAYOUT = CardDemoLayouts.TRANSACTION;

    private final JdbcTemplate jdbc;

    public InterestTransactionReader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The run's transactions, encoded with the code page, in seq_no order. */
    public List<byte[]> records(long runId, CodePage codePage) {
        RecordSource source = RecordSource.built(TABLE);
        String columns = String.join(", ", LAYOUT.fields().stream().map(InterestTransactionReader::column).toList());
        return jdbc.query("SELECT " + columns + " FROM " + TABLE + " WHERE run_id = ? ORDER BY seq_no",
                (rs, rowNumber) -> encode(rs, codePage, source), runId);
    }

    public long count(long runId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + TABLE + " WHERE run_id = ?", Long.class, runId);
    }

    /** SUM(tran_amt) of the run with the scale of TRAN-AMT; zero for a run without transactions. */
    public BigDecimal amountSum(long runId) {
        BigDecimal sum = jdbc.queryForObject("SELECT COALESCE(SUM(tran_amt), 0) FROM " + TABLE + " WHERE run_id = ?",
                BigDecimal.class, runId);
        return sum.setScale(LAYOUT.field("TRAN-AMT").scale());
    }

    private static String column(LayoutField field) {
        return field.kind() == FieldKind.FILLER ? FILLER_COLUMN : field.name().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static byte[] encode(ResultSet rs, CodePage codePage, RecordSource source) throws SQLException {
        byte[] record = new byte[LAYOUT.length()];
        int column = 1;
        for (LayoutField field : LAYOUT.fields()) {
            Object value = switch (field.kind()) {
                case FILLER -> rs.getBytes(column);
                case TEXT -> rs.getString(column);
                default -> rs.getBigDecimal(column);
            };
            record = LAYOUT.replace(record, field.name(), value, codePage, source);
            column++;
        }
        return record;
    }
}
