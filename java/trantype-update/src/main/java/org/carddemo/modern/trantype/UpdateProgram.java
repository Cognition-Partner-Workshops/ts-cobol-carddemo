package org.carddemo.modern.trantype;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

public final class UpdateProgram {
    private final Consumer<String> sysout;
    private String message = " ".repeat(80);
    private int returnCode;

    public UpdateProgram(Consumer<String> sysout) {
        this.sysout = sysout;
    }

    public int returnCode() {
        return returnCode;
    }

    public void run(Path input, Connection connection) throws SQLException, IOException {
        connection.setAutoCommit(false);
        sysout.accept("DSNRUN: DSN ATTACHED, RUNNING COBTUPDT");
        try (InputStream records = Files.newInputStream(input)) {
            sysout.accept("OPEN FILE OK");
            byte[] bytes;
            while ((bytes = records.readNBytes(53)).length > 0) {
                String record = new String(bytes, StandardCharsets.ISO_8859_1);
                process((record + " ".repeat(53)).substring(0, 53), connection);
            }
        }
        connection.commit();
        sysout.accept("DSNRUN: COMMIT AT END OF TASK");
    }

    void process(String record, Connection connection) {
        sysout.accept("PROCESSING   " + record);
        char operation = record.charAt(0);
        if (operation == '*') {
            sysout.accept("IGNORING COMMENTED LINE");
            return;
        }
        String sql;
        String success;
        switch (operation) {
            case 'A' -> {
                sysout.accept("ADDING RECORD");
                sql = "INSERT INTO carddemo.transaction_type (tr_description, tr_type) VALUES (?, ?)";
                success = "INSERTED";
            }
            case 'U' -> {
                sysout.accept("UPDATING RECORD");
                sql = "UPDATE carddemo.transaction_type SET tr_description = ? WHERE tr_type = ?";
                success = "UPDATED";
            }
            case 'D' -> {
                sysout.accept("DELETING RECORD");
                sql = "DELETE FROM carddemo.transaction_type WHERE tr_type = ?";
                success = "DELETED";
            }
            default -> {
                error("ERROR: TYPE NOT VALID");
                return;
            }
        }
        try (var statement = connection.prepareStatement(sql)) {
            int keyParameter = 1;
            if (operation != 'D') {
                statement.setString(1, record.substring(3, 53));
                keyParameter = 2;
            }
            statement.setString(keyParameter, record.substring(1, 3));
            statement.executeUpdate();
            sysout.accept("RECORD " + success + " SUCCESSFULLY");
        } catch (SQLException exception) {
            error("Error accessing: TRANSACTION_TYPE table. SQLCODE:"
                    + String.format("%5d", sqlcode(exception)));
        }
    }

    static int sqlcode(SQLException exception) {
        return switch (exception.getSQLState() == null ? "" : exception.getSQLState()) {
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

    private void error(String text) {
        message = text + message.substring(text.length());
        sysout.accept(message);
        returnCode = 4;
    }
}
