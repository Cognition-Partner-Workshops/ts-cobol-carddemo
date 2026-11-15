package com.carddemo.interest.jdbc.load;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.codec.RecordLayout.FieldLayout;
import com.carddemo.interest.codec.RecordLayout.Kind;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.load.DecodedExtract;
import com.carddemo.interest.load.ExtractFile;
import com.carddemo.interest.load.LoadFigures;

/**
 * The replica table writes of loadExtractsStep (architecture.md sections 3 and 5): TRUNCATE and
 * every INSERT of one table. The caller holds the transaction. Columns are bound with the type of
 * the data map: NUMERIC from long and BigDecimal, CHAR from the text, BYTEA from the FILLER bytes (E4).
 */
public final class ReplicaTableWriter {

    private static final int BATCH_SIZE = 1000;

    private final JdbcTemplate jdbc;

    public ReplicaTableWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void replace(DecodedExtract extract) {
        ExtractFile file = extract.file();
        List<FieldLayout> fields = file.layout().fields();
        jdbc.execute("TRUNCATE TABLE " + file.table());
        String insert = "INSERT INTO " + file.table() + " (" + String.join(", ", file.columns()) + ") VALUES ("
                + fields.stream().map(field -> "?").collect(Collectors.joining(", ")) + ")";
        jdbc.batchUpdate(insert, extract.rows(), BATCH_SIZE, (PreparedStatement statement, RecordValues row) -> {
            for (int index = 0; index < fields.size(); index++) {
                FieldLayout field = fields.get(index);
                switch (field.kind()) {
                    case UNSIGNED -> statement.setLong(index + 1, row.unsigned(field.name()));
                    case SIGNED -> statement.setBigDecimal(index + 1, row.signed(field.name()));
                    case TEXT -> statement.setString(index + 1, row.text(field.name()));
                    case FILLER -> statement.setBytes(index + 1, row.filler(index));
                    case REFUSED -> throw new IllegalStateException("field " + field.name() + " has no column");
                }
            }
        });
    }

    /** RC1 and RC2 as the table holds them: the row count and the sum of each of the given columns. */
    public LoadFigures figures(ExtractFile file, List<String> summedColumns) {
        List<String> selected = new ArrayList<>(List.of("count(*)"));
        summedColumns.forEach(column -> selected.add("coalesce(sum(" + column + "), 0)"));
        return jdbc.queryForObject("SELECT " + String.join(", ", selected) + " FROM " + file.table(), (row, n) -> {
            Map<String, BigDecimal> sums = new LinkedHashMap<>();
            for (int index = 0; index < summedColumns.size(); index++) {
                sums.put(summedColumns.get(index), row.getBigDecimal(index + 2));
            }
            return new LoadFigures(row.getLong(1), sums);
        });
    }

    /** RC4: the lowest (or highest) key of the table in the order of its primary key (E6: collation C). */
    public List<Object> edgeKey(ExtractFile file, boolean isHighest) {
        String direction = isHighest ? " DESC" : " ASC";
        String order = file.keyColumns().stream().map(column -> column + direction).collect(Collectors.joining(", "));
        List<List<Object>> keys = jdbc.query(
                "SELECT " + String.join(", ", file.keyColumns()) + " FROM " + file.table() + " ORDER BY " + order + " LIMIT 1",
                (row, n) -> {
                    List<Object> key = new ArrayList<>();
                    for (String field : file.keyFields()) {
                        int column = key.size() + 1;
                        key.add(file.layout().field(field).kind() == Kind.UNSIGNED
                                ? (Object) row.getLong(column) : row.getString(column).stripTrailing());
                    }
                    return key;
                });
        return keys.isEmpty() ? List.of() : keys.get(0);
    }
}
