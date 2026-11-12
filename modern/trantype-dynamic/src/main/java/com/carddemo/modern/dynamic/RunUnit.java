package com.carddemo.modern.dynamic;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.carddemo.modern.codec.Field;
import org.carddemo.modern.codec.RecordLayout;
import org.springframework.stereotype.Component;

/** DSNRUN and DSNEXIT: one business connection, committed only at end of task. */
@Component
public class RunUnit {
    private static final RecordLayout SQL_LINE = new RecordLayout("DYNSQL",
            List.of(Field.text("SQL", 0, 72)));
    Connection connection;
    Path caseDir;
    String program;
    int rc;
    final List<String> sysout = new ArrayList<>();

    void open(Path fixture, Path estate) throws Exception {
        caseDir = fixture;
        connection = DriverManager.getConnection("jdbc:postgresql://" + env("PGHOST", "localhost")
                + ":" + env("PGPORT", "5432") + "/" + env("PGDATABASE", "carddemo"),
                env("PGUSER", "carddemo"), env("PGPASSWORD", "carddemo"));
        try (var statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE carddemo.transaction_type CASCADE");
            statement.execute("TRUNCATE TABLE carddemo.transaction_type_categor CASCADE");
            Path seed = fixture.resolve("input/db/seed.sql");
            if (Files.exists(seed)) statement.execute(Files.readString(seed));
        }
        connection.setAutoCommit(false);
        var matcher = Pattern.compile("PARMS\\('([^']*)'\\)").matcher(Files.readString(
                estate.resolve("app/app-transaction-type-db2/jcl/MNTTRDYN.jcl")));
        if (!matcher.find()) throw new IllegalArgumentException("Missing dynamic CALL parameter");
        program = pad(matcher.group(1), 8).stripTrailing();
        sysout.add("DSNRUN: DSN ATTACHED, RUNNING CBTRNDYN");
    }

    /** 1000-READ-STATEMENT: comment lines skipped, columns 73-80 ignored. */
    static String assemble(List<String> lines) {
        StringBuilder sql = new StringBuilder();
        for (String line : lines) {
            if (!line.startsWith("--")) sql.append(SQL_LINE.encode(SQL_LINE.decode(line))).append(' ');
        }
        return sql.substring(0, Math.min(800, sql.length()));
    }

    /** 2000-RUN-DYNAMIC-SQL: ocesql defers server-side prepare until EXECUTE. */
    void dynamicSql() throws Exception {
        List<String> lines = read("DYNSQL");
        long count = lines.stream().filter(line -> !line.startsWith("--")).count() % 100;
        sysout.add(String.format(Locale.ROOT, "CBTRNDYN: %02d SQL LINES READ", count));
        String sql = assemble(lines);
        int code = 0;
        if (sql.isBlank()) {
            code = -212;
            rc = 8;
        } else try (var statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException error) {
            code = sqlcode(error);
            rc = 8;
        }
        sysout.add("CBTRNDYN: EXECUTE SQLCODE " + formatCode(code));
        sysout.add("CBTRNDYN: CALLING " + pad(program, 8));
    }

    List<String> read(String file) throws Exception {
        Path path = caseDir.resolve("input/" + file + ".txt");
        return Files.exists(path) ? Files.readAllLines(path, StandardCharsets.ISO_8859_1) : List.of();
    }

    /** DSNEXIT after COBTUPDT STOP RUN: commit without resuming CBTRNDYN's END display. */
    void finish() throws SQLException {
        sysout.add("DSNRUN: COMMIT AT END OF TASK");
        connection.commit();
    }

    static String pad(String text, int length) {
        return (text + " ".repeat(length)).substring(0, length);
    }

    static String formatCode(int code) {
        return String.format(Locale.ROOT, "%+011d", code);
    }

    static int sqlcode(SQLException error) {
        return "23505".equals(error.getSQLState()) ? -403 : -400;
    }

    private static String env(String name, String fallback) {
        return System.getenv().getOrDefault(name, fallback);
    }
}
