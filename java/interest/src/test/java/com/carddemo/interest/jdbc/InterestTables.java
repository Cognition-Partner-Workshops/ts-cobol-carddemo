package com.carddemo.interest.jdbc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.AccountRecord;
import com.carddemo.interest.codec.CardXrefRecord;
import com.carddemo.interest.codec.CategoryBalanceRecord;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.DisclosureGroupRecord;
import com.carddemo.interest.codec.FieldSpec;
import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.codec.TransactionRecord;

/**
 * Test support for calcInterestStep on the tables (ticket 06). The loader (ticket 05) and the
 * exporter (ticket 07) are not part of this ticket, so the tests put the fixture input into the
 * replica tables and read the result back as records themselves, through the record codec of
 * ticket 03. Nothing here is used by the job.
 */
public final class InterestTables {

    public static final String TRANSACTIONS = "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt";
    public static final String ACCOUNTS = "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt";
    public static final String RUN_LOG = "sysout/INTCALC.STEP15.txt";
    public static final String RETURN_CODES = "rc.json";

    /** The three account columns interest-calc may write (architecture.md section 3). */
    private static final String ACCOUNT_WITHOUT_WRITTEN_COLUMNS =
            "to_jsonb(t) - 'acct_curr_bal' - 'acct_curr_cyc_credit' - 'acct_curr_cyc_debit'";

    private static final CodePage CODE_PAGE = CodePage.fixture();

    private record Replica(String table, String file, RecordLayout layout) {
    }

    private static final List<Replica> REPLICAS = List.of(
            new Replica("carddemo.account", "AWS.M2.CARDDEMO.ACCTDATA.PS.txt", AccountRecord.LAYOUT),
            new Replica("carddemo.card_xref", "AWS.M2.CARDDEMO.CARDXREF.PS.txt", CardXrefRecord.LAYOUT),
            new Replica("carddemo.tran_cat_balance", "AWS.M2.CARDDEMO.TCATBALF.PS.txt", CategoryBalanceRecord.LAYOUT),
            new Replica("carddemo.disclosure_group", "AWS.M2.CARDDEMO.DISCGRP.PS.txt", DisclosureGroupRecord.LAYOUT));

    private InterestTables() {
    }

    /** postgres:15 with the two schema scripts of db/postgres, as the db service of docker-compose.yml applies them (S7.3, FI-4). */
    public static PostgreSQLContainer<?> postgres() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:15")
                .withDatabaseName("carddemo")
                .withUsername("carddemo")
                .withPassword("carddemo");
        for (String script : List.of("01-transaction-type.sql", "02-interest.sql")) {
            container.withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("..", "..", "db", "postgres", script)),
                    "/docker-entrypoint-initdb.d/" + script);
        }
        return container;
    }

    /** Empties the six tables of the stream. */
    public static void clear(JdbcTemplate jdbc) {
        jdbc.execute("TRUNCATE carddemo.interest_transaction, carddemo.interest_run, carddemo.account,"
                + " carddemo.card_xref, carddemo.tran_cat_balance, carddemo.disclosure_group");
    }

    /** Replaces the four replica tables by the records of the input directories, in the order given. */
    public static void load(JdbcTemplate jdbc, Path... inputDirs) {
        clear(jdbc);
        for (Path inputDir : inputDirs) {
            for (Replica replica : REPLICAS) {
                insert(jdbc, replica, Fixtures.lines(inputDir.resolve(replica.file())));
            }
        }
    }

    /** Adds balance records, given as lines of the unloaded file, to carddemo.tran_cat_balance. */
    public static void insertBalances(JdbcTemplate jdbc, List<String> lines) {
        insert(jdbc, REPLICAS.get(2), lines);
    }

    private static void insert(JdbcTemplate jdbc, Replica replica, List<String> lines) {
        RecordCodec codec = new RecordCodec(replica.layout(), CODE_PAGE);
        String sql = "INSERT INTO " + replica.table() + " VALUES ("
                + "?, ".repeat(replica.layout().fields().size() - 1) + "?) ON CONFLICT DO NOTHING";
        int recordNumber = 0;
        for (String line : lines) {
            RecordSource source = new RecordSource(replica.file(), ++recordNumber);
            RecordValues values = codec.decode(RecordImage.fromLine(line, replica.layout().length(), CODE_PAGE), source);
            List<Object> columns = new ArrayList<>();
            for (FieldSpec field : replica.layout().fields()) {
                columns.add(switch (field.kind()) {
                    case UNSIGNED_ZONED -> values.unsigned(field);
                    case SIGNED_ZONED -> values.signed(field);
                    case TEXT -> values.text(field);
                    case FILLER -> values.filler(field);
                    case NOT_DISPLAY -> throw new IllegalStateException(field.name());
                });
            }
            jdbc.update(sql, columns.toArray());
        }
    }

    /** The interest transactions of a run as 350-byte records in seq_no order, one line each (recorded format). */
    public static List<String> transactionLines(JdbcTemplate jdbc, long runId) {
        return jdbc.query("""
                SELECT tran_id, tran_type_cd, tran_cat_cd, tran_source, tran_desc, tran_amt, tran_merchant_id,
                       tran_merchant_name, tran_merchant_city, tran_merchant_zip, tran_card_num, tran_orig_ts,
                       tran_proc_ts, tran_filler
                  FROM carddemo.interest_transaction WHERE run_id = ? ORDER BY seq_no""",
                (row, n) -> line(TransactionRecord.LAYOUT, row, "interest_transaction", n), runId);
    }

    /** Every account as a 300-byte record in key order, one line each (recorded format). */
    public static List<String> accountLines(JdbcTemplate jdbc) {
        return jdbc.query("SELECT * FROM carddemo.account ORDER BY acct_id",
                (row, n) -> line(AccountRecord.LAYOUT, row, "account", n));
    }

    private static String line(RecordLayout layout, ResultSet row, String table, int rowNumber) throws SQLException {
        RecordValues values = RecordValues.of(layout);
        for (FieldSpec field : layout.fields()) {
            int column = field.index() + 1;
            values = switch (field.kind()) {
                case UNSIGNED_ZONED -> values.withUnsigned(field, row.getBigDecimal(column).longValueExact());
                case SIGNED_ZONED -> values.withSigned(field, row.getBigDecimal(column));
                case TEXT -> values.withText(field, row.getString(column));
                case FILLER -> values.withFiller(field, row.getBytes(column));
                case NOT_DISPLAY -> throw new IllegalStateException(field.name());
            };
        }
        return RecordImage.toLine(new RecordCodec(layout, CODE_PAGE).encode(values, new RecordSource(table, rowNumber + 1)), CODE_PAGE);
    }

    /**
     * Writes the tables of a finished run as the directory tools/parity/compare.py reads: the two
     * datasets from the tables, the run log the step wrote (already in outputDir) and the return
     * codes of a run that reached its normal end, in the recorded shape (pitfall P4).
     */
    public static void writeActual(JdbcTemplate jdbc, long runId, Path outputDir) {
        try {
            Files.createDirectories(outputDir.resolve("datasets"));
            Files.writeString(outputDir.resolve(TRANSACTIONS), text(transactionLines(jdbc, runId)), CODE_PAGE.charset());
            Files.writeString(outputDir.resolve(ACCOUNTS), text(accountLines(jdbc)), CODE_PAGE.charset());
            Files.writeString(outputDir.resolve(RETURN_CODES), """
                    {
                     "INTCALC": {
                      "maxrc": 0,
                      "steps": {
                       "STEP05": 0,
                       "STEP10": 0,
                       "STEP15": 0,
                       "STEP20": 0,
                       "STEP25": 0,
                       "STEP30": 0
                      }
                     }
                    }""", CODE_PAGE.charset());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String text(List<String> lines) {
        StringBuilder text = new StringBuilder();
        lines.forEach(line -> text.append(line).append('\n'));
        return text.toString();
    }

    /** One hash over every row of a table: every column, every row. */
    public static String rowHash(JdbcTemplate jdbc, String table) {
        return jdbc.queryForObject("SELECT coalesce(md5(string_agg(md5(t::text), '' ORDER BY t::text)), 'empty') FROM "
                + table + " t", String.class);
    }

    /** One hash over every account row without the three columns the calculation may write. */
    public static String accountHashWithoutWrittenColumns(JdbcTemplate jdbc) {
        return jdbc.queryForObject("SELECT coalesce(md5(string_agg(md5((" + ACCOUNT_WITHOUT_WRITTEN_COLUMNS
                + ")::text), '' ORDER BY acct_id)), 'empty') FROM carddemo.account t", String.class);
    }

    /**
     * Records every statement-level write on the replica tables: each UPDATE of account with the
     * account id and whether a column other than the three allowed ones changed, and any write at
     * all on tran_cat_balance, card_xref and disclosure_group. Table carddemo_test.write_audit.
     */
    public static void installWriteAudit(JdbcTemplate jdbc) {
        jdbc.execute("""
                CREATE SCHEMA IF NOT EXISTS carddemo_test;
                CREATE TABLE IF NOT EXISTS carddemo_test.write_audit
                (   table_name TEXT NOT NULL, operation TEXT NOT NULL, acct_id NUMERIC(11,0), other_column_changed BOOLEAN);
                CREATE OR REPLACE FUNCTION carddemo_test.audit_account() RETURNS trigger AS $$
                BEGIN
                    INSERT INTO carddemo_test.write_audit VALUES ('account', TG_OP, NEW.acct_id,
                        (to_jsonb(OLD) - 'acct_curr_bal' - 'acct_curr_cyc_credit' - 'acct_curr_cyc_debit')
                        IS DISTINCT FROM
                        (to_jsonb(NEW) - 'acct_curr_bal' - 'acct_curr_cyc_credit' - 'acct_curr_cyc_debit'));
                    RETURN NEW;
                END $$ LANGUAGE plpgsql;
                CREATE OR REPLACE FUNCTION carddemo_test.audit_replica() RETURNS trigger AS $$
                BEGIN
                    INSERT INTO carddemo_test.write_audit VALUES (TG_TABLE_NAME, TG_OP, NULL, NULL);
                    RETURN NULL;
                END $$ LANGUAGE plpgsql;
                DROP TRIGGER IF EXISTS audit_account ON carddemo.account;
                CREATE TRIGGER audit_account AFTER UPDATE ON carddemo.account
                    FOR EACH ROW EXECUTE FUNCTION carddemo_test.audit_account();
                DROP TRIGGER IF EXISTS audit_replica ON carddemo.tran_cat_balance;
                CREATE TRIGGER audit_replica AFTER INSERT OR UPDATE OR DELETE ON carddemo.tran_cat_balance
                    FOR EACH STATEMENT EXECUTE FUNCTION carddemo_test.audit_replica();
                DROP TRIGGER IF EXISTS audit_replica ON carddemo.card_xref;
                CREATE TRIGGER audit_replica AFTER INSERT OR UPDATE OR DELETE ON carddemo.card_xref
                    FOR EACH STATEMENT EXECUTE FUNCTION carddemo_test.audit_replica();
                DROP TRIGGER IF EXISTS audit_replica ON carddemo.disclosure_group;
                CREATE TRIGGER audit_replica AFTER INSERT OR UPDATE OR DELETE ON carddemo.disclosure_group
                    FOR EACH STATEMENT EXECUTE FUNCTION carddemo_test.audit_replica();
                TRUNCATE carddemo_test.write_audit""");
    }

    public static void removeWriteAudit(JdbcTemplate jdbc) {
        jdbc.execute("""
                DROP TRIGGER IF EXISTS audit_account ON carddemo.account;
                DROP TRIGGER IF EXISTS audit_replica ON carddemo.tran_cat_balance;
                DROP TRIGGER IF EXISTS audit_replica ON carddemo.card_xref;
                DROP TRIGGER IF EXISTS audit_replica ON carddemo.disclosure_group""");
    }

    /** Lines "table operation acct_id other_column_changed" of the audit, in a stable order. */
    public static List<String> writeAudit(JdbcTemplate jdbc) {
        return jdbc.queryForList("""
                SELECT table_name || ' ' || operation || ' ' || coalesce(lpad(acct_id::text, 11, '0'), '-') || ' '
                       || coalesce(other_column_changed::text, '-')
                  FROM carddemo_test.write_audit ORDER BY 1""", String.class);
    }

    /**
     * Fault injection in the database: the statement named fails once it is reached. {@code when}
     * is a trigger WHEN condition on NEW, for example {@code NEW.seq_no = 2}.
     */
    public static void failOn(JdbcTemplate jdbc, String operation, String table, String when) {
        jdbc.execute("""
                CREATE SCHEMA IF NOT EXISTS carddemo_test;
                CREATE OR REPLACE FUNCTION carddemo_test.forced_failure() RETURNS trigger AS $$
                BEGIN
                    RAISE EXCEPTION 'forced failure of the test';
                END $$ LANGUAGE plpgsql;
                DROP TRIGGER IF EXISTS forced_failure ON %2$s;
                CREATE TRIGGER forced_failure BEFORE %1$s ON %2$s
                    FOR EACH ROW WHEN (%3$s) EXECUTE FUNCTION carddemo_test.forced_failure()""".formatted(operation, table, when));
    }

    public static void stopFailingOn(JdbcTemplate jdbc, String table) {
        jdbc.execute("DROP TRIGGER IF EXISTS forced_failure ON " + table);
    }
}
