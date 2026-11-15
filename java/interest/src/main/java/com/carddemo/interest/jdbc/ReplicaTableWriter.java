package com.carddemo.interest.jdbc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.carddemo.interest.codec.DecodedRecord;
import com.carddemo.interest.codec.LayoutField;

/**
 * Replaces the content of one replica table: TRUNCATE, then one INSERT per record, as IDCAMS REPRO
 * into an emptied cluster does (architecture, section Transaction boundaries). The caller owns the
 * transaction; both statements belong to it.
 */
@Repository
public class ReplicaTableWriter {

    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate jdbc;

    public ReplicaTableWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void replace(ReplicaTable table, List<DecodedRecord> records) {
        jdbc.execute("TRUNCATE TABLE " + table.tableName());
        String insert = "INSERT INTO " + table.tableName() + " (" + table.columnList() + ") VALUES ("
                + String.join(", ", Collections.nCopies(table.layout().fields().size(), "?")) + ")";
        List<Object[]> rows = new ArrayList<>(Math.min(records.size(), BATCH_SIZE));
        for (DecodedRecord record : records) {
            rows.add(values(table, record));
            if (rows.size() == BATCH_SIZE) {
                jdbc.batchUpdate(insert, rows);
                rows.clear();
            }
        }
        if (!rows.isEmpty()) {
            jdbc.batchUpdate(insert, rows);
        }
    }

    private static Object[] values(ReplicaTable table, DecodedRecord record) {
        List<LayoutField> fields = table.layout().fields();
        Object[] values = new Object[fields.size()];
        for (int i = 0; i < values.length; i++) {
            LayoutField field = fields.get(i);
            values[i] = switch (field.kind()) {
                case FILLER -> record.filler(field.name());
                case TEXT -> record.text(field.name());
                default -> record.number(field.name());
            };
        }
        return values;
    }
}
