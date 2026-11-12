package com.carddemo.modern.trantype;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

/**
 * The DSN attach of {@code //STEP1 EXEC PGM=IKJEFT01} ({@code DSN SYSTEM(DAZ1) / RUN PROGRAM(COBTUPDT)}),
 * off-host the stubs tools/stubs/DSNRUN.cbl and DSNEXIT.cbl: one connection and one unit of work for
 * the whole task, committed at end of task. Like ocesql (which issues BEGIN after CONNECT) the
 * connection is not in autocommit, so after the first failed statement PostgreSQL aborts the unit
 * of work: later statements fail with SQLSTATE 25P02 (SQLCODE -400) and the end-of-task COMMIT
 * rolls everything back while still reporting success.
 */
@Component
public class Db2Attach implements TransactionTypeSql {

    static final String INSERT = "INSERT INTO carddemo.transaction_type (tr_type, tr_description) VALUES (?, ?)";
    static final String UPDATE = "UPDATE carddemo.transaction_type SET tr_description = ? WHERE tr_type = ?";
    static final String DELETE = "DELETE FROM carddemo.transaction_type WHERE tr_type = ?";

    private final DataSource dataSource;
    private final Sysout sysout;
    private Connection connection;

    public Db2Attach(DataSource dataSource, Sysout sysout) {
        this.dataSource = dataSource;
        this.sysout = sysout;
    }

    /** DSNRUN: CONNECT, then DISPLAY 'DSNRUN: DSN ATTACHED, RUNNING ' WS-PGM-NAME (PIC X(8)). */
    public void attach(String program) {
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            sysout.display("DSNRUN: DATABASE CONNECT FAILED " + displaySqlcode(Sqlca.sqlcode(e)));
            throw new IllegalStateException("DSN attach failed", e);
        }
        sysout.display("DSNRUN: DSN ATTACHED, RUNNING " + String.format("%-8.8s", program));
    }

    /** DSNEXIT (exit procedure at STOP RUN): EXEC SQL COMMIT, then DISCONNECT ALL. */
    public void commitAtEndOfTask() {
        if (connection == null) {
            return;
        }
        int sqlcode = Sqlca.OK;
        try {
            connection.commit();
        } catch (SQLException e) {
            sqlcode = Sqlca.sqlcode(e);
        }
        sysout.display(sqlcode == Sqlca.OK ? "DSNRUN: COMMIT AT END OF TASK" : "DSNRUN: COMMIT FAILED " + displaySqlcode(sqlcode));
        try {
            connection.close();
        } catch (SQLException ignored) {
            // DISCONNECT ALL does not report
        } finally {
            connection = null;
        }
    }

    @Override
    public int insert(String trType, String description) {
        return execute(INSERT, trType, description);
    }

    @Override
    public int update(String trType, String description) {
        return execute(UPDATE, description, trType);
    }

    @Override
    public int delete(String trType) {
        return execute(DELETE, trType);
    }

    /**
     * ocesql connects with client_encoding OCDB_DB_CHAR, default SJIS (ocesql.c), and sends the
     * PIC X bytes as they are, so the server decodes them as SJIS; pgJDBC itself is UTF-8 only.
     */
    private static final Charset OCDB_DB_CHAR = Charset.forName("Shift_JIS");
    static final String SERVER_DECODE = "SELECT convert_from(?, 'SJIS')";

    /** Host variables are bound as the full PIC X value, trailing spaces included, like ocesql. */
    private int execute(String sql, String... hostVariables) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < hostVariables.length; i++) {
                ps.setString(i + 1, asClientEncoding(hostVariables[i]));
            }
            ps.executeUpdate();
            return Sqlca.OK;
        } catch (SQLException e) {
            return Sqlca.sqlcode(e);
        }
    }

    /**
     * The record's bytes (read as Latin-1, one char per byte) as the server decodes them. Bytes
     * that are not valid SJIS go to the server to decode, so that its error (SQLSTATE 22021,
     * SQLCODE -400) aborts the unit of work exactly as the legacy statement does.
     */
    private String asClientEncoding(String picX) throws SQLException {
        Optional<String> decoded = decodeSjis(picX);
        if (decoded.isPresent()) {
            return decoded.get();
        }
        try (PreparedStatement ps = connection.prepareStatement(SERVER_DECODE)) {
            ps.setBytes(1, picX.getBytes(StandardCharsets.ISO_8859_1));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    static Optional<String> decodeSjis(String picX) {
        try {
            return Optional.of(OCDB_DB_CHAR.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(picX.getBytes(StandardCharsets.ISO_8859_1)))
                    .toString());
        } catch (CharacterCodingException e) {
            return Optional.empty();
        }
    }

    /** DISPLAY of SQLCODE (PIC S9(9) COMP-5): sign and nine digits. Not covered by a fixture. */
    private static String displaySqlcode(int sqlcode) {
        return (sqlcode < 0 ? "-" : "+") + String.format("%09d", Math.abs((long) sqlcode));
    }
}
