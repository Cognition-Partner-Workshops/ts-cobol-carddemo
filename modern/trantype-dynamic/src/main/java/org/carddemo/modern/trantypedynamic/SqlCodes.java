package org.carddemo.modern.trantypedynamic;

import java.sql.SQLException;

/** SQLCA SQLCODE values and the two ways the programs DISPLAY them. */
final class SqlCodes {
    static final int NOT_FOUND = 100;

    private SqlCodes() {
    }

    /**
     * The SQLCODE the estate's precompiler (Open COBOL ESQL 1.4, dblib/ocpgsql.c) sets for a failed
     * PostgreSQL statement: a handful of SQLSTATEs have their own code, everything else is -400.
     */
    static int of(SQLException e) {
        var state = e.getSQLState();
        if (state == null) return -9999;
        return switch (state) {
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

    /**
     * DISPLAY SQLCODE. The ocesql SQLCA declares SQLCODE PIC S9(9) COMP-5, and GnuCOBOL displays a
     * COMP-5 item with the digits of its four-byte binary capacity: sign and ten digits.
     */
    static String display(int code) {
        return (code < 0 ? "-" : "+") + String.format("%010d", Math.abs((long) code));
    }

    /** MOVE SQLCODE TO WS-VAR-SQLCODE PIC ----9. */
    static String edited(int code) {
        var digits = Long.toString(Math.abs((long) code) % 10_000L);
        return String.format("%5s", (code < 0 ? "-" : "") + digits);
    }
}
