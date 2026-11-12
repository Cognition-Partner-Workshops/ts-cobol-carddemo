package org.carddemo.modern.trantypedynamic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
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
            rc = program.call();
        }
        sysout.add("CBTRNDYN: END");
        return rc;
    }

    /** STRING ... INTO WS-SQL-STMT-ARR WITH POINTER: characters past PIC X(800) are dropped. */
    static void append(StringBuilder statement, String piece) {
        int room = STMT_CAPACITY - statement.length();
        statement.append(piece, 0, Math.min(room, piece.length()));
    }

    /**
     * PREPARE/EXECUTE through ocesql: no savepoint is taken (OCDB_PG_IGNORE_ERROR is not set), so a
     * failed statement leaves the PostgreSQL transaction aborted for everything that follows.
     */
    private int runDynamicSql(String text) throws SQLException {
        java.sql.PreparedStatement prepared;
        try {
            prepared = unit.prepareStatement(text);
        } catch (SQLException e) {
            sysout.add("CBTRNDYN: PREPARE FAILED SQLCODE " + SqlCodes.display(SqlCodes.of(e)));
            return 8;
        }
        try (prepared) {
            // ocesql sets +100 only for a query with no rows; DML that touches no row is SQLCODE 0.
            int code = 0;
            if (prepared.execute()) {
                try (var rows = prepared.getResultSet()) {
                    if (!rows.next()) code = SqlCodes.NOT_FOUND;
                }
            }
            sysout.add("CBTRNDYN: EXECUTE SQLCODE " + SqlCodes.display(code));
            return 0;
        } catch (SQLException e) {
            sysout.add("CBTRNDYN: EXECUTE SQLCODE " + SqlCodes.display(SqlCodes.of(e)));
            return 8;
        }
    }
}
