package com.carddemo.modern.trantype;

import java.sql.SQLException;

/**
 * SQLCODE as the legacy run sees it. MNTTRDB2 runs off-host on Open COBOL ESQL 1.4 against
 * PostgreSQL, so SQLCODE is what {@code OCDB_PGSetResultStatus} (dblib/ocpgsql.c) derives from
 * the PostgreSQL result, not what Db2 would return:
 * <ul>
 *   <li>INSERT/UPDATE/DELETE that executes is {@code PGRES_COMMAND_OK} -> SQLCODE 0, whatever the
 *       row count. A DELETE or UPDATE that matches no row is therefore SQLCODE 0, never +100 (Db2
 *       returns +100 here);</li>
 *   <li>errors map by SQLSTATE: 23505 unique violation -> -403 ({@code OCPG_DUPLICATE_KEY}), the
 *       other listed states to their OCPG code, anything else -> -400 ({@code OCPG_PGSQL}), e.g.
 *       23503 foreign key and 25P02 "current transaction is aborted"; no SQLSTATE -> -9999.</li>
 * </ul>
 */
final class Sqlca {

    static final int OK = 0;
    static final int NOT_FOUND = 100;

    private Sqlca() {
    }

    static int sqlcode(SQLException e) {
        String state = e.getSQLState();
        if (state == null) {
            return -9999;
        }
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
}
