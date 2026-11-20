package com.carddemo.trantypedynamic;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

final class DynamicMaintenance {
    record SqlInput(String statement, int lines) {}

    static SqlInput assemble(List<String> records) {
        StringBuilder sql = new StringBuilder();
        int lines = 0;
        for (String record : records) {
            if (!record.startsWith("--")) {
                sql.append(fixed(record, 72)).append(' ');
                lines = (lines + 1) % 100;
            }
        }
        return new SqlInput(sql.substring(0, Math.min(sql.length(), 800)), lines);
    }

    static String fixed(String value, int width) {
        return (value + " ".repeat(width)).substring(0, width);
    }

    private final PrintStream out;

    DynamicMaintenance(PrintStream out) {
        this.out = out;
    }

    int run(Connection connection, SqlInput input, String program, List<String> updates) throws SQLException {
        out.println("DSNRUN: DSN ATTACHED, RUNNING CBTRNDYN");
        out.printf("CBTRNDYN: %02d SQL LINES READ%n", input.lines());
        int code;
        try (var statement = connection.createStatement()) {
            boolean rows = statement.execute(input.statement());
            code = hasSqlToken(input.statement()) ? 0 : -212;
            if (rows) {
                try (var result = statement.getResultSet()) {
                    code = result.next() ? 0 : 100;
                }
            }
        } catch (SQLException failure) {
            code = sqlcode(failure);
        }
        out.printf("CBTRNDYN: EXECUTE SQLCODE %+011d%n", code);
        int rc = code < 0 ? 8 : 0;
        if (!program.isEmpty()) {
            program = fixed(program, 8);
            out.println("CBTRNDYN: CALLING " + program);
            if (!program.stripTrailing().equals("COBTUPDT")) {
                throw new IllegalArgumentException("Unsupported dynamic CALL: " + program);
            }
            rc = apply(connection, updates);
        } else {
            out.println("CBTRNDYN: END");
        }
        out.println("DSNRUN: COMMIT AT END OF TASK");
        connection.commit();
        return rc;
    }

    private int apply(Connection connection, List<String> updates) {
        out.println("OPEN FILE OK");
        int rc = 0;
        StringBuilder errorBuffer = new StringBuilder(" ".repeat(80));
        for (String line : updates) {
            String record = fixed(line, 53);
            out.println("PROCESSING   " + record);
            char operation = record.charAt(0);
            if (operation == '*') {
                out.println("IGNORING COMMENTED LINE");
                continue;
            }
            String sql;
            String success;
            switch (operation) {
                case 'A' -> {
                    out.println("ADDING RECORD");
                    sql = "INSERT INTO carddemo.transaction_type (tr_description, tr_type) VALUES (?, ?)";
                    success = "INSERTED";
                }
                case 'U' -> {
                    out.println("UPDATING RECORD");
                    sql = "UPDATE carddemo.transaction_type SET tr_description = ? WHERE tr_type = ?";
                    success = "UPDATED";
                }
                case 'D' -> {
                    out.println("DELETING RECORD");
                    sql = "DELETE FROM carddemo.transaction_type WHERE tr_type = ?";
                    success = "DELETED";
                }
                default -> {
                    String message = "ERROR: TYPE NOT VALID";
                    errorBuffer.replace(0, message.length(), message);
                    out.println(errorBuffer);
                    rc = 4;
                    continue;
                }
            }
            try (var statement = connection.prepareStatement(sql)) {
                int keyParameter = 1;
                if (operation != 'D') {
                    statement.setString(1, record.substring(3));
                    keyParameter = 2;
                }
                statement.setString(keyParameter, record.substring(1, 3));
                statement.executeUpdate();
                out.println("RECORD " + success + " SUCCESSFULLY");
            } catch (SQLException failure) {
                String message = "Error accessing: TRANSACTION_TYPE table. SQLCODE:"
                        + String.format("%5d", sqlcode(failure));
                errorBuffer.replace(0, message.length(), message);
                out.println(errorBuffer);
                rc = 4;
            }
        }
        return rc;
    }

    static int sqlcode(SQLException failure) {
        if (failure.getSQLState() == null) {
            return -9999;
        }
        return switch (failure.getSQLState()) {
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

    static boolean hasSqlToken(String sql) {
        int position = 0;
        while (position < sql.length()) {
            char ch = sql.charAt(position);
            if (Character.isWhitespace(ch) || ch == ';') {
                position++;
            } else if (sql.startsWith("--", position)) {
                int newline = sql.indexOf('\n', position + 2);
                position = newline < 0 ? sql.length() : newline + 1;
            } else if (sql.startsWith("/*", position)) {
                int depth = 1;
                position += 2;
                while (position < sql.length() && depth > 0) {
                    if (sql.startsWith("/*", position)) {
                        depth++;
                        position += 2;
                    } else if (sql.startsWith("*/", position)) {
                        depth--;
                        position += 2;
                    } else {
                        position++;
                    }
                }
                if (depth != 0) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }
}
