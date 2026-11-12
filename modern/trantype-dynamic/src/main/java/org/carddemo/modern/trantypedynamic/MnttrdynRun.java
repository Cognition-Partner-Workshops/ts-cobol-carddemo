package org.carddemo.modern.trantypedynamic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.carddemo.modern.codec.Layouts;
import org.carddemo.modern.codec.RecordLayout;

/**
 * The DSN command processor around CBTRNDYN (tools/stubs/DSNRUN.cbl + DSNEXIT.cbl): attach, run the
 * program in one unit of work, commit at end of task, then export the recorder's contract.
 */
final class MnttrdynRun {
    static final String JOB = "MNTTRDYN";
    static final String STEP = "STEP1";
    static final String PROGRAM = "CBTRNDYN";
    private final DataSource dataSource;

    MnttrdynRun(DataSource dataSource) { this.dataSource = dataSource; }

    int run(Path input, Path output, String parms) throws IOException, SQLException {
        TrantypeTables.reset(dataSource, input.resolve("db/seed.sql"));
        var sysout = new ArrayList<String>();
        sysout.add("DSNRUN: DSN ATTACHED, RUNNING " + PROGRAM);
        int rc;
        try (var unit = dataSource.getConnection()) {
            unit.setAutoCommit(false);
            var updates = read(input.resolve("INPFILE.txt"), Layouts.TRANTYPE_UPDATE);
            Map<String, Subprogram> loadlib = Map.of("COBTUPDT", new Cobtupdt(unit, sysout, updates));
            try {
                rc = new Cbtrndyn(unit, sysout, loadlib).run(read(input.resolve("DYNSQL.txt"), Layouts.DYNSQL), parms);
            } catch (StopRun stop) {
                rc = stop.returnCode;
            }
            // DSNEXIT's EXEC SQL COMMIT: on an aborted transaction PostgreSQL answers ROLLBACK with no
            // error, so ocesql reports SQLCODE 0 and the work of the whole step is discarded.
            try {
                unit.commit();
                sysout.add("DSNRUN: COMMIT AT END OF TASK");
            } catch (SQLException e) {
                sysout.add("DSNRUN: COMMIT FAILED " + SqlCodes.display(SqlCodes.of(e)));
            }
        }
        Files.createDirectories(output.resolve("sysout"));
        Files.write(output.resolve("sysout/" + JOB + "." + STEP + ".txt"), sysout, StandardCharsets.ISO_8859_1);
        Files.writeString(output.resolve("rc.json"), "{\n \"" + JOB + "\": {\n  \"maxrc\": " + rc
                + ",\n  \"steps\": {\n   \"" + STEP + "\": " + rc + "\n  }\n }\n}");
        TrantypeTables.export(dataSource, output.resolve("db"));
        return rc;
    }

    /** A sequential dataset as record.py materialises it: each line padded or cut to the LRECL. */
    static List<Map<String, Object>> read(Path path, RecordLayout layout) throws IOException {
        if (!Files.exists(path)) return List.of();
        return Files.readAllLines(path, StandardCharsets.ISO_8859_1).stream().map(layout::decode).toList();
    }
}
