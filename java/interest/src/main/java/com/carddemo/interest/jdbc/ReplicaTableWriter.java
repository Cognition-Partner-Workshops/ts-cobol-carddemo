package com.carddemo.interest.jdbc;

import com.carddemo.interest.codec.FieldSpec;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.load.ExtractFile;
import com.carddemo.interest.load.TableFigures;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The loader's writes to the four replica tables (architecture.md section 3: interest-loader,
 * truncate and insert) and the reads that prove them. It opens no transaction: the caller runs
 * {@link #replace} and the checks of one table in one transaction (architecture.md section 5).
 * Column names and types are those of db/postgres/02-interest.sql (FI-4).
 */
public final class ReplicaTableWriter {

    private static final int INSERT_BATCH_SIZE = 1000;

    private final JdbcTemplate jdbc;

    public ReplicaTableWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Empties the table and inserts the rows, as IDCAMS REPRO into an emptied cluster. */
    public void replace(ExtractFile file, List<RecordValues> rows) {
        List<FieldSpec> fields = file.layout().fields();
        jdbc.execute("TRUNCATE TABLE " + file.qualifiedTable());
        String sql = "INSERT INTO " + file.qualifiedTable() + " (" + columns(file, fields) + ") VALUES ("
                + fields.stream().map(field -> "?").collect(Collectors.joining(", ")) + ")";
        jdbc.batchUpdate(sql, rows, INSERT_BATCH_SIZE, (statement, row) -> bind(statement, fields, row));
    }

    /** RC1, RC2, RC4 on the table side: count, distinct keys, sums of the amounts, hash total. */
    public TableFigures figures(ExtractFile file) {
        List<String> summedColumns = new ArrayList<>();
        file.summedFields().forEach(field -> summedColumns.add(file.column(field)));
        file.hashTotalField().ifPresent(field -> summedColumns.add(file.column(field)));
        StringBuilder sql = new StringBuilder("SELECT count(*), count(DISTINCT (")
                .append(columns(file, file.keyFields())).append("))");
        for (String column : summedColumns) {
            sql.append(", coalesce(sum(").append(column).append("), 0)");
        }
        sql.append(" FROM ").append(file.qualifiedTable());
        return jdbc.queryForObject(sql.toString(), (resultSet, rowNumber) -> {
            Map<String, BigDecimal> sums = new LinkedHashMap<>();
            for (int i = 0; i < summedColumns.size(); i++) {
                sums.put(summedColumns.get(i), resultSet.getBigDecimal(i + 3));
            }
            return new TableFigures(resultSet.getLong(1), resultSet.getLong(2), sums);
        });
    }

    /** Every row of the table as record values, in key order, for the round trip (RC3). */
    public List<RecordValues> rowsInKeyOrder(ExtractFile file) {
        List<FieldSpec> fields = file.layout().fields();
        String sql = "SELECT " + columns(file, fields) + " FROM " + file.qualifiedTable()
                + " ORDER BY " + columns(file, file.keyFields());
        return jdbc.query(sql, (resultSet, rowNumber) -> row(resultSet, file, fields));
    }

    private static String columns(ExtractFile file, List<FieldSpec> fields) {
        return fields.stream().map(file::column).collect(Collectors.joining(", "));
    }

    private static void bind(PreparedStatement statement, List<FieldSpec> fields, RecordValues row) throws SQLException {
        for (FieldSpec field : fields) {
            int parameter = field.index() + 1;
            switch (field.kind()) {
                case UNSIGNED_ZONED -> statement.setLong(parameter, row.unsigned(field));
                case SIGNED_ZONED -> statement.setBigDecimal(parameter, row.signed(field));
                case TEXT -> statement.setString(parameter, row.text(field));
                case FILLER -> statement.setBytes(parameter, row.filler(field));
                case NOT_DISPLAY -> throw new IllegalStateException("field " + field.name() + " has no column (E5)");
            }
        }
    }

    private static RecordValues row(ResultSet resultSet, ExtractFile file, List<FieldSpec> fields) throws SQLException {
        RecordValues values = RecordValues.of(file.layout());
        for (FieldSpec field : fields) {
            int column = field.index() + 1;
            values = switch (field.kind()) {
                case UNSIGNED_ZONED -> values.withUnsigned(field, resultSet.getLong(column));
                case SIGNED_ZONED -> values.withSigned(field, resultSet.getBigDecimal(column));
                // CHAR(n) comes back padded; the codec holds text without trailing spaces (E3).
                case TEXT -> values.withText(field, resultSet.getString(column).stripTrailing());
                case FILLER -> values.withFiller(field, resultSet.getBytes(column));
                case NOT_DISPLAY -> throw new IllegalStateException("field " + field.name() + " has no column (E5)");
            };
        }
        return values;
    }
}
