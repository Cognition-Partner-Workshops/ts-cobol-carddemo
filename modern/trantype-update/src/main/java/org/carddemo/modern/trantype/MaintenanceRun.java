package org.carddemo.modern.trantype;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import org.carddemo.modern.codec.TrantypeInput;
import org.postgresql.PGConnection;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.ByteArrayResource;

final class MaintenanceRun {
    private final JdbcTemplate jdbc;

    MaintenanceRun(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    int run(Path input, Path output) throws IOException {
        Files.createDirectories(output.resolve("datasets"));
        Files.createDirectories(output.resolve("sysout"));
        Files.createDirectories(output.resolve("db"));
        var lines = new ArrayList<String>();
        int rc = 0;
        try {
            jdbc.execute("TRUNCATE maintenance_input, transaction_type_categor, transaction_type");
            var seed = input.resolve("db/seed.sql");
            if (Files.exists(seed)) {
                var schema = jdbc.queryForObject("SELECT current_schema()", String.class);
                var sql = Files.readString(seed).replace("carddemo.", schema + ".");
                jdbc.execute((ConnectionCallback<Void>) connection -> {
                    ScriptUtils.executeSqlScript(connection, new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
                    return null;
                });
            }
            long sequence = 0;
            var inputFile = input.resolve("INPFILE.txt");
            var inputLines = Files.exists(inputFile)
                    ? Files.readAllLines(inputFile, StandardCharsets.ISO_8859_1) : java.util.List.<String>of();
            for (var line : inputLines) {
                var record = TrantypeInput.LAYOUT.decode(line);
                jdbc.update("INSERT INTO maintenance_input VALUES (?, ?, ?, ?)", ++sequence,
                        record.get("INPUT-REC-TYPE"), record.get("INPUT-REC-NUMBER"), record.get("INPUT-REC-DESC"));
            }
            lines.add("OPEN FILE OK");
            lines.addFirst("DSNRUN: DSN ATTACHED, RUNNING COBTUPDT");
            var rows = jdbc.queryForList("SELECT * FROM maintenance_input ORDER BY record_number");
            rc = jdbc.execute((ConnectionCallback<Integer>) connection -> {
                boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                int returnCode = 0;
                String returnMessage = " ".repeat(80);
                try {
                    for (var row : rows) {
                        var record = Map.<String, Object>of("INPUT-REC-TYPE", row.get("input_rec_type"),
                                "INPUT-REC-NUMBER", row.get("input_rec_number"), "INPUT-REC-DESC", row.get("input_rec_desc"));
                        lines.add("PROCESSING   " + TrantypeInput.LAYOUT.encode(record));
                        var type = row.get("input_rec_number");
                        var description = row.get("input_rec_desc").toString();
                        try {
                            switch (row.get("input_rec_type").toString()) {
                                case "*" -> lines.add("IGNORING COMMENTED LINE");
                                case "A" -> {
                                    lines.add("ADDING RECORD");
                                    execute(connection, "INSERT INTO transaction_type VALUES (?, ?)", type, description);
                                    lines.add("RECORD INSERTED SUCCESSFULLY");
                                }
                                case "U", "D" -> {
                                    boolean update = row.get("input_rec_type").equals("U");
                                    lines.add(update ? "UPDATING RECORD" : "DELETING RECORD");
                                    if (update) execute(connection, "UPDATE transaction_type SET tr_description = ? WHERE tr_type = ?", description, type);
                                    else execute(connection, "DELETE FROM transaction_type WHERE tr_type = ?", type);
                                    // Legacy defect (t05): ocesql PGRES_COMMAND_OK yields SQLCODE 0 even for zero rows;
                                    // COBTUPDT 10032/10033 therefore never reach their +100 error branches.
                                    lines.add(update ? "RECORD UPDATED SUCCESSFULLY" : "RECORD DELETED SUCCESSFULLY");
                                }
                                default -> {
                                    returnMessage = overwrite(returnMessage, "ERROR: TYPE NOT VALID");
                                    lines.add(returnMessage);
                                    returnCode = 4;
                                }
                            }
                        } catch (SQLException error) {
                            returnMessage = overwrite(returnMessage, sqlErrorMessage(error.getSQLState()).stripTrailing());
                            lines.add(returnMessage);
                            returnCode = 4;
                        }
                    }
                    try {
                        connection.commit();
                        lines.add("DSNRUN: COMMIT AT END OF TASK");
                    } catch (SQLException error) {
                        lines.add("DSNRUN: COMMIT FAILED " + String.format(java.util.Locale.ROOT, "%+011d", sqlErrorCode(error.getSQLState())));
                    }
                    return returnCode;
                } catch (RuntimeException error) {
                    connection.rollback();
                    throw error;
                } finally {
                    connection.setAutoCommit(autoCommit);
                }
            });
        } catch (IOException | RuntimeException error) {
            lines.add("MAINTENANCE FAILED: " + error.getMessage());
            rc = 12;
        }
        Files.write(output.resolve("sysout/MNTTRDB2.STEP1.txt"), lines, StandardCharsets.ISO_8859_1);
        Files.writeString(output.resolve("rc.json"), "{\n \"MNTTRDB2\": {\n  \"maxrc\": " + rc
                + ",\n  \"steps\": {\n   \"STEP1\": " + rc + "\n  }\n }\n}");
        snapshot(output, "transaction_type", "tr_type");
        snapshot(output, "transaction_type_categor", "trc_type_code, trc_type_category");
        return rc;
    }

    static String sqlErrorMessage(String state) {
        // COBTUPDT 10031: PIC ----9 SQLCODE in PIC X(80) WS-RETURN-MSG.
        return String.format(java.util.Locale.ROOT, "%-80s",
                "Error accessing: TRANSACTION_TYPE table. SQLCODE:" + String.format(java.util.Locale.ROOT, "%5d", sqlErrorCode(state)));
    }

    private static int sqlErrorCode(String state) {
        return switch (state == null ? "" : state) {
            case "08001" -> -402;
            case "08003" -> -220;
            case "08007" -> -401;
            case "21000" -> -404;
            case "23505" -> -403;
            case "25001" -> -603;
            case "25P01" -> -604;
            case "34000" -> -602;
            case "42804" -> -204;
            case "42P03" -> -605;
            default -> -400;
        };
    }

    private static String overwrite(String previous, String message) {
        int length = Math.min(message.length(), previous.length());
        return message.substring(0, length) + previous.substring(length);
    }

    private static void execute(Connection connection, String sql, Object... values) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            statement.executeUpdate();
        }
    }

    private void snapshot(Path output, String table, String order) throws IOException {
        String csv = jdbc.execute((ConnectionCallback<String>) connection -> {
            var writer = new StringWriter();
            try {
                connection.unwrap(PGConnection.class).getCopyAPI().copyOut(
                        "COPY (SELECT * FROM " + table + " ORDER BY " + order + ") TO STDOUT WITH CSV HEADER", writer);
            } catch (IOException error) {
                throw new java.io.UncheckedIOException(error);
            }
            return writer.toString();
        });
        Files.writeString(output.resolve("db/carddemo." + table + ".csv"), csv);
    }
}
