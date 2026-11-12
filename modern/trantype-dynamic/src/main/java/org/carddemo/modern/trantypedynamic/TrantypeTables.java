package org.carddemo.modern.trantypedynamic;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.postgresql.PGConnection;

/** Candidate tables in the trantype_dynamic schema and the legacy table names their snapshots stand for. */
record TrantypeTables(String name, String legacyName, String order) {
    static final TrantypeTables TRANSACTION_TYPE = new TrantypeTables("transaction_type", "carddemo.transaction_type", "tr_type");
    static final TrantypeTables CATEGORY = new TrantypeTables("transaction_type_categor", "carddemo.transaction_type_categor",
            "trc_type_code, trc_type_category");
    static final List<TrantypeTables> ALL = List.of(TRANSACTION_TYPE, CATEGORY);
    private static final Pattern LEGACY_SCHEMA = Pattern.compile("(?i)\\bcarddemo\\.");

    /** record.py reset_database: truncate, then load the case's seed into the candidate schema. */
    static void reset(DataSource dataSource, Path seed) throws IOException, SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE " + TRANSACTION_TYPE.name + ", " + CATEGORY.name + " CASCADE");
            if (Files.exists(seed)) statement.execute(LEGACY_SCHEMA.matcher(Files.readString(seed)).replaceAll(""));
        }
    }

    /** record.py snapshot_database: COPY ... ORDER BY ... TO STDOUT WITH CSV HEADER per table. */
    static void export(DataSource dataSource, Path directory) throws IOException, SQLException {
        Files.createDirectories(directory);
        try (var connection = dataSource.getConnection()) {
            var copy = connection.unwrap(PGConnection.class).getCopyAPI();
            for (var table : ALL) {
                var csv = new StringWriter();
                copy.copyOut("COPY (SELECT * FROM " + table.name + " ORDER BY " + table.order + ") TO STDOUT WITH CSV HEADER", csv);
                Files.writeString(directory.resolve(table.legacyName + ".csv"), csv.toString());
            }
        }
    }
}
