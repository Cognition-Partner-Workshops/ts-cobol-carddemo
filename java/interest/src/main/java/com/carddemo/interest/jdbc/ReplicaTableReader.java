package com.carddemo.interest.jdbc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.LayoutField;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;

/**
 * Reads a replica table back as records of its copybook, in key order: the round trip of
 * reconciliation RC3 and the ACCTDATA image of the export. Read only.
 */
@Repository
public class ReplicaTableReader {

    private final JdbcTemplate jdbc;

    public ReplicaTableReader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every row, encoded with the code page, ordered by the primary key. */
    public List<byte[]> records(ReplicaTable table, CodePage codePage) {
        RecordSource source = RecordSource.built(table.tableName());
        return jdbc.query("SELECT " + table.columnList() + " FROM " + table.tableName() + " ORDER BY " + table.keyOrder(),
                (rs, rowNumber) -> encode(table, rs, codePage, source));
    }

    public long count(ReplicaTable table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table.tableName(), Long.class);
    }

    /** SUM of a numeric column over the table, with the scale of its field; zero for an empty table. */
    public BigDecimal sum(ReplicaTable table, String fieldName) {
        BigDecimal sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(" + table.column(fieldName) + "), 0) FROM " + table.tableName(), BigDecimal.class);
        return sum.setScale(table.layout().field(fieldName).scale());
    }

    private static byte[] encode(ReplicaTable table, ResultSet rs, CodePage codePage, RecordSource source) throws SQLException {
        RecordLayout layout = table.layout();
        byte[] record = new byte[layout.length()];
        int column = 1;
        for (LayoutField field : layout.fields()) {
            Object value = switch (field.kind()) {
                case FILLER -> rs.getBytes(column);
                case TEXT -> rs.getString(column);
                default -> rs.getBigDecimal(column);
            };
            record = layout.replace(record, field.name(), value, codePage, source);
            column++;
        }
        return record;
    }
}
