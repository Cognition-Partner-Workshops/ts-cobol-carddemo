package org.carddemo.modern.trantypedynamic;

import java.sql.SQLException;

/** SQLCA SQLCODE values and the two ways the programs DISPLAY them. */
final class SqlCodes {
    static final int NOT_FOUND = 100;

    private SqlCodes() {
    }

    /** The Db2 SQLCODE for a PostgreSQL SQLSTATE. */
    static int of(SQLException e) {
        var state = e.getSQLState() == null ? "" : e.getSQLState();
        return switch (state) {
            case "42703" -> -206;
            case "42P01" -> -204;
            case "42601" -> -104;
            case "23505" -> -803;
            case "23503" -> -530;
            case "23502" -> -407;
            case "22001" -> -404;
            default -> -901;
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
