package org.carddemo.modern.codec;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

/** Sends PIC X bytes through PostgreSQL's SJIS decoder, including server-side encoding failures. */
public final class SjisSql {
    private SjisSql() {
    }

    /** SQL consists of fixed DML with positional host-variable placeholders. */
    public static int executeUpdate(Connection connection, String sql, String... hostVariables) throws SQLException {
        try (var statement = connection.prepareStatement(sql.replace("?", "convert_from(?, 'SJIS')"))) {
            for (int i = 0; i < hostVariables.length; i++) {
                statement.setBytes(i + 1, hostVariables[i].getBytes(StandardCharsets.ISO_8859_1));
            }
            return statement.executeUpdate();
        }
    }
}
