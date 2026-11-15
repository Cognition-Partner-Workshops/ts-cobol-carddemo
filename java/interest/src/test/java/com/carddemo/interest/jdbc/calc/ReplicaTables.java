package com.carddemo.interest.jdbc.calc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import com.carddemo.interest.codec.AccountRecord;
import com.carddemo.interest.codec.CardXrefRecord;
import com.carddemo.interest.codec.CategoryBalanceRecord;
import com.carddemo.interest.codec.DisclosureGroupRecord;
import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordFormat;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordLayout.FieldLayout;
import com.carddemo.interest.codec.RecordOrigin;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.codec.TransactionRecord;

/**
 * Test support of ticket 06: PostgreSQL 15 with the schema of FI-4 (standards S7.3), the replica
 * tables filled from fixture input files, and the tables read back as the fixture file content the
 * recorded legacy run is compared with. It stands in for the loader (ticket 05) and the export
 * (ticket 07) in these tests only; column names follow the data map (field name in lower case).
 */
public final class ReplicaTables {

    /** A table of schema carddemo, the layout of its rows, its FILLER column and its key order. */
    public record Table(String name, RecordLayout layout, String fillerColumn, String keyOrder, String inputFile) {
    }

    public static final Table ACCOUNT = new Table("carddemo.account", AccountRecord.LAYOUT, "acct_filler",
            "acct_id", "AWS.M2.CARDDEMO.ACCTDATA.PS.txt");
    public static final Table CARD_XREF = new Table("carddemo.card_xref", CardXrefRecord.LAYOUT, "xref_filler",
            "xref_card_num COLLATE \"C\"", "AWS.M2.CARDDEMO.CARDXREF.PS.txt");
    public static final Table BALANCE = new Table("carddemo.tran_cat_balance", CategoryBalanceRecord.LAYOUT,
            "trancat_filler", "trancat_acct_id, trancat_type_cd COLLATE \"C\", trancat_cd", "AWS.M2.CARDDEMO.TCATBALF.PS.txt");
    public static final Table RATE = new Table("carddemo.disclosure_group", DisclosureGroupRecord.LAYOUT, "dis_filler",
            "dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd", "AWS.M2.CARDDEMO.DISCGRP.PS.txt");
    public static final Table TRANSACTION = new Table("carddemo.interest_transaction", TransactionRecord.LAYOUT,
            "tran_filler", "run_id, seq_no", null);
    private static final List<Table> REPLICAS = List.of(ACCOUNT, CARD_XREF, BALANCE, RATE);

    private static final RecordFormat FORMAT = RecordFormat.fixture();
    private static final Path DDL = Path.of("..", "..", "db", "postgres");

    private ReplicaTables() {
    }

    /** postgres:15 as the db service of docker-compose.yml starts it: both init scripts of db/postgres. */
    public static PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:15")
                .withDatabaseName("carddemo")
                .withUsername("carddemo")
                .withPassword("carddemo")
                .withCopyFileToContainer(MountableFile.forHostPath(DDL.resolve("01-transaction-type.sql")),
                        "/docker-entrypoint-initdb.d/01-transaction-type.sql")
                .withCopyFileToContainer(MountableFile.forHostPath(DDL.resolve("02-interest.sql")),
                        "/docker-entrypoint-initdb.d/02-interest.sql");
    }

    /** Empties the six tables of the stream and fills the four replica tables from each input directory in turn. */
    public static void load(JdbcTemplate jdbc, Path... inputDirs) {
        jdbc.execute("TRUNCATE carddemo.interest_transaction, carddemo.interest_run, carddemo.account,"
                + " carddemo.card_xref, carddemo.tran_cat_balance, carddemo.disclosure_group");
        for (Path inputDir : inputDirs) {
            for (Table table : REPLICAS) {
                insertFile(jdbc, table, inputDir.resolve(table.inputFile()));
            }
        }
    }

    private static void insertFile(JdbcTemplate jdbc, Table table, Path file) {
        RecordCodec codec = FORMAT.codec(table.layout());
        int recordNumber = 0;
        for (byte[] record : FORMAT.records(readAllBytes(file), table.layout(), file.getFileName().toString())) {
            recordNumber++;
            insert(jdbc, table, codec.decode(record, new RecordOrigin(file.getFileName().toString(), recordNumber)));
        }
    }

    /** Inserts one row; a row that is already there (the rates are the same in every case) is left alone. */
    public static void insert(JdbcTemplate jdbc, Table table, RecordValues values) {
        List<String> columns = new ArrayList<>();
        List<Object> arguments = new ArrayList<>();
        for (int i = 0; i < table.layout().fields().size(); i++) {
            FieldLayout field = table.layout().fields().get(i);
            switch (field.kind()) {
                case UNSIGNED -> arguments.add(values.unsigned(field.name()));
                case SIGNED -> arguments.add(values.signed(field.name()));
                case TEXT -> arguments.add(values.text(field.name()));
                case FILLER -> arguments.add(values.filler(i) == null ? spaces(field.length()) : values.filler(i));
                case REFUSED -> throw new IllegalStateException(field.name());
            }
            columns.add(column(table, field));
        }
        jdbc.update("INSERT INTO " + table.name() + " (" + String.join(", ", columns) + ") VALUES ("
                + columns.stream().map(column -> "?").collect(Collectors.joining(", ")) + ") ON CONFLICT DO NOTHING",
                arguments.toArray());
    }

    private static byte[] spaces(int length) {
        return " ".repeat(length).getBytes(FORMAT.codePage().charset());
    }

    private static String column(Table table, FieldLayout field) {
        return field.kind() == RecordLayout.Kind.FILLER
                ? table.fillerColumn()
                : field.name().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    /** The rows of a table in key order as the content of a fixture file (FI-2). */
    public static String fileContent(JdbcTemplate jdbc, Table table, String where, Object... arguments) {
        RecordCodec codec = FORMAT.codec(table.layout());
        List<byte[]> records = jdbc.query(
                "SELECT * FROM " + table.name() + " " + where + " ORDER BY " + table.keyOrder(), (rs, rowNumber) -> {
                    RecordValues values = RecordValues.blank(table.layout());
                    byte[] filler = null;
                    FieldLayout fillerField = null;
                    for (FieldLayout field : table.layout().fields()) {
                        String column = column(table, field);
                        switch (field.kind()) {
                            case UNSIGNED -> values = values.with(field.name(), rs.getLong(column));
                            case SIGNED -> values = values.with(field.name(), rs.getBigDecimal(column));
                            case TEXT -> values = values.with(field.name(), rs.getString(column));
                            case FILLER -> {
                                filler = rs.getBytes(column);
                                fillerField = field;
                            }
                            case REFUSED -> throw new IllegalStateException(field.name());
                        }
                    }
                    byte[] record = codec.encode(values, new RecordOrigin(table.name(), rowNumber + 1));
                    System.arraycopy(filler, 0, record, fillerField.offset(), fillerField.length());
                    return record;
                }, arguments);
        return new String(FORMAT.content(records), FORMAT.codePage().charset());
    }

    /** One hash over every row of a table, to show that a run left it as it was. */
    public static String rowHash(JdbcTemplate jdbc, Table table) {
        return jdbc.queryForObject("SELECT coalesce(md5(string_agg(t::text, '|' ORDER BY t::text)), 'empty') FROM "
                + table.name() + " t", String.class);
    }

    /** The hash of account without the three columns the calculation may update (architecture.md section 3). */
    public static String accountHashWithoutPostedColumns(JdbcTemplate jdbc) {
        return jdbc.queryForObject("SELECT coalesce(md5(string_agg(j::text, '|' ORDER BY j::text)), 'empty') FROM ("
                + "SELECT to_jsonb(a) - 'acct_curr_bal' - 'acct_curr_cyc_credit' - 'acct_curr_cyc_debit' AS j"
                + " FROM carddemo.account a) rows", String.class);
    }

    /**
     * A trigger that refuses any UPDATE statement on account whose SET list names a column other
     * than the three of INT-R07: PostgreSQL fires an UPDATE OF trigger only for the columns named.
     */
    public static void refuseUpdatesOfOtherAccountColumns(JdbcTemplate jdbc) {
        jdbc.execute("""
                CREATE OR REPLACE FUNCTION carddemo.test_refuse_account_column() RETURNS trigger AS $$
                BEGIN RAISE EXCEPTION 'an UPDATE names a column of account the calculation may not write'; END
                $$ LANGUAGE plpgsql""");
        jdbc.execute("DROP TRIGGER IF EXISTS test_other_columns ON carddemo.account");
        jdbc.execute("""
                CREATE TRIGGER test_other_columns BEFORE UPDATE OF acct_id, acct_active_status, acct_credit_limit,
                    acct_cash_credit_limit, acct_open_date, acct_expiraion_date, acct_reissue_date, acct_addr_zip,
                    acct_group_id, acct_filler
                ON carddemo.account FOR EACH STATEMENT EXECUTE FUNCTION carddemo.test_refuse_account_column()""");
    }

    private static byte[] readAllBytes(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
