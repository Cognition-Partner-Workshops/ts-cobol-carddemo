package org.carddemo.modern.trantypedynamic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** CBTRNDYN: run the DYNSQL statement with PREPARE/EXECUTE, then CALL the program named in PARMS. */
final class Cbtrndyn {
    static final int STMT_CAPACITY = 800;
    private final Connection unit;
    private final List<String> sysout;
    private final Map<String, Subprogram> loadlib;

    Cbtrndyn(Connection unit, List<String> sysout, Map<String, Subprogram> loadlib) {
        this.unit = unit;
        this.sysout = sysout;
        this.loadlib = loadlib;
    }

    int run(List<Map<String, Object>> dynsql, String parms) throws SQLException {
        var statement = new StringBuilder();
        int lines = 0;
        for (var record : dynsql) {
            var text = record.get("DYNSQL-REC").toString();
            if (text.startsWith("--")) continue;
            append(statement, text.substring(0, 72) + " ");
            lines++;
        }
        sysout.add("CBTRNDYN: " + String.format("%02d", lines % 100) + " SQL LINES READ");
        int rc = runDynamicSql(statement.toString());
        if (parms != null && !parms.isEmpty()) {
            var name = String.format("%-8.8s", parms);
            sysout.add("CBTRNDYN: CALLING " + name);
            var program = loadlib.get(name.strip());
            if (program == null) throw new IllegalStateException("CBTRNDYN: CALL " + name.strip() + ": program not found");
            rc = Math.max(rc, program.call());
        }
        sysout.add("CBTRNDYN: END");
        return rc;
    }

    /** STRING ... INTO WS-SQL-STMT-ARR WITH POINTER: characters past PIC X(800) are dropped. */
    static void append(StringBuilder statement, String piece) {
        int room = STMT_CAPACITY - statement.length();
        statement.append(piece, 0, Math.min(room, piece.length()));
    }

    private int runDynamicSql(String text) throws SQLException {
        var savepoint = unit.setSavepoint();
        java.sql.PreparedStatement prepared;
        try {
            prepared = unit.prepareStatement(text);
        } catch (SQLException e) {
            unit.rollback(savepoint);
            sysout.add("CBTRNDYN: PREPARE FAILED SQLCODE " + SqlCodes.display(SqlCodes.of(e)));
            return 8;
        }
        try (prepared) {
            boolean query = prepared.execute();
            int code = !query && prepared.getUpdateCount() == 0 && changesRows(text) ? SqlCodes.NOT_FOUND : 0;
            unit.releaseSavepoint(savepoint);
            sysout.add("CBTRNDYN: EXECUTE SQLCODE " + SqlCodes.display(code));
            return 0;
        } catch (SQLException e) {
            unit.rollback(savepoint);
            sysout.add("CBTRNDYN: EXECUTE SQLCODE " + SqlCodes.display(SqlCodes.of(e)));
            return 8;
        }
    }

    private static boolean changesRows(String text) {
        var verb = text.strip().split("\\s+", 2)[0].toUpperCase(Locale.ROOT);
        return verb.equals("UPDATE") || verb.equals("DELETE") || verb.equals("INSERT");
    }
}
