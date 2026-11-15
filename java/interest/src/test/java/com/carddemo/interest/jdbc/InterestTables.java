package com.carddemo.interest.jdbc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.Fixtures;
import com.carddemo.interest.codec.AccountRecord;
import com.carddemo.interest.codec.CardXrefRecord;
import com.carddemo.interest.codec.CategoryBalanceRecord;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.DecodedRecord;
import com.carddemo.interest.codec.DisclosureGroupRecord;
import com.carddemo.interest.codec.FieldKind;
import com.carddemo.interest.codec.LayoutField;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.codec.TransactionRecord;

/**
 * Test support for the tables of db/postgres/02-interest.sql (FI-4). Puts the input files of a
 * fixture case into the four replica tables - the loader is ticket 05 and is not used here - and
 * compares table rows with recorded legacy records field by field. A column is the copybook field
 * name in lower case with underscores; the FILLER column carries the table's prefix (data map).
 */
public final class InterestTables {

    private static final RecordSource SOURCE = RecordSource.built("test");

    private record Table(String name, String fillerColumn, RecordLayout layout, String inputFile) {
    }

    private static final List<Table> REPLICAS = List.of(
            new Table("carddemo.account", "acct_filler", AccountRecord.LAYOUT, "AWS.M2.CARDDEMO.ACCTDATA.PS.txt"),
            new Table("carddemo.card_xref", "xref_filler", CardXrefRecord.LAYOUT, "AWS.M2.CARDDEMO.CARDXREF.PS.txt"),
            new Table("carddemo.tran_cat_balance", "trancat_filler", CategoryBalanceRecord.LAYOUT, "AWS.M2.CARDDEMO.TCATBALF.PS.txt"),
            new Table("carddemo.disclosure_group", "dis_filler", DisclosureGroupRecord.LAYOUT, "AWS.M2.CARDDEMO.DISCGRP.PS.txt"));

    private InterestTables() {
    }

    /** Replaces the four replica tables with the input files of a directory in the fixture format. */
    public static void load(JdbcTemplate jdbc, Path inputDir) {
        for (Table table : REPLICAS) {
            jdbc.execute("TRUNCATE " + table.name());
            for (String line : Fixtures.lines(inputDir.resolve(table.inputFile()))) {
                insert(jdbc, table, line);
            }
        }
    }

    /** Adds one record, given as a fixture line, to a replica table. */
    public static void insertBalance(JdbcTemplate jdbc, String line) {
        insert(jdbc, REPLICAS.get(2), line);
    }

    public static void insertCard(JdbcTemplate jdbc, String line) {
        insert(jdbc, REPLICAS.get(1), line);
    }

    private static void insert(JdbcTemplate jdbc, Table table, String line) {
        DecodedRecord record = decode(table.layout(), line);
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (LayoutField field : table.layout().fields()) {
            columns.add(column(field, table.fillerColumn()));
            values.add(value(record, field));
        }
        jdbc.update("INSERT INTO " + table.name() + " (" + String.join(", ", columns) + ") VALUES ("
                + String.join(", ", columns.stream().map(column -> "?").toList()) + ")", values.toArray());
    }

    /** Asserts that carddemo.account holds exactly the records of a recorded ACCTDATA image, field by field. */
    public static void assertAccountsEqual(JdbcTemplate jdbc, List<String> recordedLines) {
        assertRowsEqual(jdbc.queryForList("SELECT * FROM carddemo.account ORDER BY acct_id"),
                recordedLines, AccountRecord.LAYOUT, "acct_filler");
    }

    /** Asserts that the run's rows of carddemo.interest_transaction, in seq_no order, equal recorded SYSTRAN records. */
    public static void assertTransactionsEqual(JdbcTemplate jdbc, long interestRunId, List<String> recordedLines) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM carddemo.interest_transaction WHERE run_id = ? ORDER BY seq_no", interestRunId);
        assertRowsEqual(rows, recordedLines, TransactionRecord.LAYOUT, "tran_filler");
        for (int i = 0; i < rows.size(); i++) {
            assertEquals(i + 1, rows.get(i).get("seq_no"), "seq_no is the write order, from 1");
        }
    }

    private static void assertRowsEqual(List<Map<String, Object>> rows, List<String> recordedLines, RecordLayout layout,
            String fillerColumn) {
        assertEquals(recordedLines.size(), rows.size(), "number of rows against recorded records of " + layout.copybook());
        for (int i = 0; i < rows.size(); i++) {
            DecodedRecord recorded = decode(layout, recordedLines.get(i));
            for (LayoutField field : layout.fields()) {
                Object actual = rows.get(i).get(column(field, fillerColumn));
                String where = layout.copybook() + " record " + (i + 1) + " " + field.name();
                if (field.kind() == FieldKind.FILLER) {
                    assertArrayEquals(recorded.filler(field.name()), (byte[]) actual, where);
                } else if (field.kind() == FieldKind.TEXT) {
                    assertEquals(recorded.text(field.name()), ((String) actual).stripTrailing(), where);
                    assertEquals(field.length(), ((String) actual).length(), where + " keeps its width");
                } else {
                    assertEquals(recorded.number(field.name()), actual, where);
                }
            }
        }
    }

    private static DecodedRecord decode(RecordLayout layout, String line) {
        return layout.decode(RecordImage.bytes(RecordImage.fromLine(line, layout, SOURCE)), CodePage.FIXTURE, SOURCE);
    }

    private static String column(LayoutField field, String fillerColumn) {
        return field.kind() == FieldKind.FILLER ? fillerColumn : field.name().toLowerCase().replace('-', '_');
    }

    private static Object value(DecodedRecord record, LayoutField field) {
        return switch (field.kind()) {
            case FILLER -> record.filler(field.name());
            case TEXT -> record.text(field.name());
            default -> record.number(field.name());
        };
    }
}
