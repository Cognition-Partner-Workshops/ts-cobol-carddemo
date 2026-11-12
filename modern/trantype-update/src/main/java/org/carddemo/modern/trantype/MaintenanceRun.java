package org.carddemo.modern.trantype;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import org.carddemo.modern.codec.TrantypeInput;
import org.postgresql.PGConnection;
import org.springframework.dao.DataAccessException;
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
            for (var line : Files.readAllLines(input.resolve("INPFILE.txt"), StandardCharsets.ISO_8859_1)) {
                if (line.length() > TrantypeInput.LAYOUT.length()) throw new IllegalArgumentException("INPFILE record exceeds 53 bytes");
                var record = TrantypeInput.LAYOUT.decode(line);
                jdbc.update("INSERT INTO maintenance_input VALUES (?, ?, ?, ?)", ++sequence,
                        record.get("INPUT-REC-TYPE"), record.get("INPUT-REC-NUMBER"), record.get("INPUT-REC-DESC"));
            }
            lines.add("OPEN FILE OK");
            lines.addFirst("DSNRUN: DSN ATTACHED, RUNNING COBTUPDT");
            for (var row : jdbc.queryForList("SELECT * FROM maintenance_input ORDER BY record_number")) {
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
                            jdbc.update("INSERT INTO transaction_type VALUES (?, ?)", type, description);
                            lines.add("RECORD INSERTED SUCCESSFULLY");
                        }
                        case "U", "D" -> {
                            boolean update = row.get("input_rec_type").equals("U");
                            lines.add(update ? "UPDATING RECORD" : "DELETING RECORD");
                            int count = update
                                    ? jdbc.update("UPDATE transaction_type SET tr_description = ? WHERE tr_type = ?", description, type)
                                    : jdbc.update("DELETE FROM transaction_type WHERE tr_type = ?", type);
                            if (count == 0) {
                                lines.add("No records found.");
                                rc = 4;
                            } else lines.add(update ? "RECORD UPDATED SUCCESSFULLY" : "RECORD DELETED SUCCESSFULLY");
                        }
                        default -> {
                            lines.add("ERROR: TYPE NOT VALID");
                            rc = 4;
                        }
                    }
                } catch (DataAccessException error) {
                    lines.add("Error accessing: TRANSACTION_TYPE table. " + error.getMostSpecificCause().getMessage());
                    rc = 4;
                }
            }
        } catch (IOException | RuntimeException error) {
            lines.add("MAINTENANCE FAILED: " + error.getMessage());
            rc = 12;
        }
        if (rc < 12) lines.add("DSNRUN: COMMIT AT END OF TASK");
        Files.write(output.resolve("sysout/MNTTRDB2.STEP1.txt"), lines, StandardCharsets.ISO_8859_1);
        Files.writeString(output.resolve("rc.json"), "{\n \"MNTTRDB2\": {\n  \"maxrc\": " + rc
                + ",\n  \"steps\": {\n   \"STEP1\": " + rc + "\n  }\n }\n}");
        snapshot(output, "transaction_type", "tr_type");
        snapshot(output, "transaction_type_categor", "trc_type_code, trc_type_category");
        return rc;
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
