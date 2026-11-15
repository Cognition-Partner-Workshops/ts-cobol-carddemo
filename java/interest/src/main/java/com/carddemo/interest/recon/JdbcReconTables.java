package com.carddemo.interest.recon;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.carddemo.interest.codec.RecordLayout.FieldLayout;
import com.carddemo.interest.codec.RecordLayout.Kind;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.load.ExtractFile;

/**
 * The report's access to schema carddemo (FI-4, db/postgres/02-interest.sql): SELECT statements
 * only, on one connection in one read-only transaction, so that the server refuses a write and all
 * figures are of one moment. Every table is named with its schema (the job's connections have
 * another search_path). Text keys are ordered in collation C (E6, standards S3.7).
 */
public final class JdbcReconTables implements ReconTables, AutoCloseable {

    private final Connection connection;
    private final JdbcTemplate jdbc;

    private JdbcReconTables(Connection connection) {
        this.connection = connection;
        this.jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
    }

    /** Opens the connection read-only; the transaction ends with a rollback in {@link #close}. */
    public static JdbcReconTables open(String url, String user, String password) throws SQLException {
        Connection connection = DriverManager.getConnection(url, user, password);
        try {
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return new JdbcReconTables(connection);
    }

    /** For the test that shows the server refuses a write on this connection. */
    Connection connection() {
        return connection;
    }

    @Override
    public Optional<RunRow> run(OptionalLong runId) {
        List<Map<String, Object>> rows = runId.isPresent()
                ? jdbc.queryForList("SELECT * FROM carddemo.interest_run WHERE run_id = ?", runId.getAsLong())
                : jdbc.queryForList("SELECT * FROM carddemo.interest_run ORDER BY run_id DESC LIMIT 1");
        return rows.stream().findFirst().map(row -> new RunRow(((Number) row.get("run_id")).longValue(),
                (String) row.get("parm_date"), (String) row.get("status"), row.entrySet().stream()
                        .filter(column -> column.getKey().startsWith("loaded_") || column.getKey().startsWith("written_"))
                        .collect(LinkedHashMap::new, (figures, column) -> figures.put(column.getKey(), column.getValue()),
                                Map::putAll)));
    }

    @Override
    public TableFigures figures(ExtractFile file, List<String> summedColumns) {
        List<String> selected = new ArrayList<>(List.of("count(*)"));
        summedColumns.forEach(column -> selected.add("sum(" + column + ")"));
        selected.add("(SELECT count(*) FROM (SELECT DISTINCT " + String.join(", ", file.keyColumns()) + " FROM "
                + file.table() + ") keys)");
        return jdbc.queryForObject("SELECT " + String.join(", ", selected) + " FROM " + file.table(), (row, n) -> {
            Map<String, BigDecimal> sums = new LinkedHashMap<>();
            for (int index = 0; index < summedColumns.size(); index++) {
                // the sum of no row is null: shown as NOT AVAILABLE, not as zero
                sums.put(summedColumns.get(index), row.getBigDecimal(index + 2));
            }
            return new TableFigures(row.getLong(1), sums, row.getLong(summedColumns.size() + 2));
        });
    }

    @Override
    public void rows(ExtractFile file, Consumer<TableRow> consumer) {
        List<FieldLayout> fields = file.layout().fields();
        String keyOrder = file.keyFields().stream().map(name -> file.layout().field(name))
                .map(field -> file.column(field) + (field.kind() == Kind.TEXT ? " COLLATE \"C\"" : ""))
                .collect(Collectors.joining(", "));
        jdbc.query("SELECT " + String.join(", ", file.columns()) + " FROM " + file.table() + " ORDER BY " + keyOrder,
                row -> {
                    RecordValues values = RecordValues.blank(file.layout());
                    byte[] filler = null;
                    for (FieldLayout field : fields) {
                        String column = file.column(field);
                        switch (field.kind()) {
                            case UNSIGNED -> values = values.with(field.name(), row.getLong(column));
                            case SIGNED -> values = values.with(field.name(), row.getBigDecimal(column));
                            case TEXT -> values = values.with(field.name(), row.getString(column));
                            case FILLER -> filler = row.getBytes(column);
                            case REFUSED -> throw new IllegalStateException("field " + field.name() + " has no column");
                        }
                    }
                    consumer.accept(new TableRow(UnloadedFiles.keyOf(file, values), values, filler));
                });
    }

    @Override
    public WrittenFigures written(long runId) {
        return jdbc.queryForObject(
                "SELECT count(*), coalesce(sum(tran_amt), 0.00) FROM carddemo.interest_transaction WHERE run_id = ?",
                (row, n) -> new WrittenFigures(row.getLong(1), row.getBigDecimal(2)), runId);
    }

    @Override
    public OptionalLong lastAccount() {
        Long account = jdbc.queryForObject("SELECT max(trancat_acct_id) FROM carddemo.tran_cat_balance", Long.class);
        return account == null ? OptionalLong.empty() : OptionalLong.of(account);
    }

    @Override
    public BigDecimal interestOfAccount(long runId, long accountId) {
        return jdbc.queryForObject("""
                SELECT coalesce(sum(t.tran_amt), 0.00)
                  FROM carddemo.interest_transaction t
                 WHERE t.run_id = ?
                   AND t.tran_card_num IN (SELECT x.xref_card_num FROM carddemo.card_xref x WHERE x.xref_acct_id = ?)
                """, BigDecimal.class, runId, accountId);
    }

    @Override
    public void close() throws SQLException {
        try {
            connection.rollback();
        } finally {
            connection.close();
        }
    }
}
