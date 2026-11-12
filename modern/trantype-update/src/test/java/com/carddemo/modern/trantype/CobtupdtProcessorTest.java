package com.carddemo.modern.trantype;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;

class CobtupdtProcessorTest {

    /** SQLCODEs the legacy (ocesql/PostgreSQL) run returns, one per statement in order. */
    private static final class ScriptedSql implements TransactionTypeSql {
        private final Deque<Integer> codes;

        ScriptedSql(Integer... codes) {
            this.codes = new ArrayDeque<>(List.of(codes));
        }

        @Override
        public int insert(String trType, String description) {
            return codes.pop();
        }

        @Override
        public int update(String trType, String description) {
            return codes.pop();
        }

        @Override
        public int delete(String trType) {
            return codes.pop();
        }
    }

    private static String pad(String s, int n) {
        return s + " ".repeat(n - s.length());
    }

    @Test
    void deleteOfMissingRowIsReportedAsDeletedWithRc0() {
        // t05-delete-missing: the zero-row DELETE comes back as SQLCODE 0 off-host, never +100
        CobtupdtProcessor p = new CobtupdtProcessor(new ScriptedSql(Sqlca.OK));
        assertEquals(List.of("PROCESSING   " + pad("D09", 53), "DELETING RECORD", "RECORD DELETED SUCCESSFULLY"),
                p.process(InputRecord.of("D09")));
        assertEquals(0, p.returnCode());
    }

    @Test
    void duplicateAddReportsSqlcodeMinus403AndRc4() {
        // t06-duplicate-add
        CobtupdtProcessor p = new CobtupdtProcessor(new ScriptedSql(-403));
        assertEquals(List.of("PROCESSING   " + pad("A01Purchase", 53), "ADDING RECORD",
                        pad("Error accessing: TRANSACTION_TYPE table. SQLCODE: -403", 80)),
                p.process(InputRecord.of("A01Purchase")));
        assertEquals(4, p.returnCode());
    }

    @Test
    void returnMessageIsOverlaidNotClearedAndRcStaysAt4() {
        CobtupdtProcessor p = new CobtupdtProcessor(new ScriptedSql(-403, Sqlca.OK));
        p.process(InputRecord.of("A01Dup"));
        p.process(InputRecord.of("A08Fine"));
        List<String> lines = p.process(InputRecord.of("X77bad"));
        assertEquals(pad("ERROR: TYPE NOT VALIDSACTION_TYPE table. SQLCODE: -403", 80), lines.get(1));
        assertEquals(4, p.returnCode());
    }

    @Test
    void nextSqlStatementResetsRc4ToZero() {
        // OCESQLEndSQL's CALL result lands in RETURN-CODE (probed against the legacy run)
        CobtupdtProcessor p = new CobtupdtProcessor(new ScriptedSql(Sqlca.OK, -403, -400));
        p.process(InputRecord.of("Q"));
        assertEquals(4, p.returnCode());
        p.process(InputRecord.of("*c"));
        assertEquals(4, p.returnCode());
        p.process(InputRecord.of("A12Twelve"));
        assertEquals(0, p.returnCode());
        p.process(InputRecord.of("A01Dup"));
        p.process(InputRecord.of("U02After failure"));
        assertEquals(4, p.returnCode());
    }

    @Test
    void notFoundBranchIsKeptForUpdateAndDelete() {
        CobtupdtProcessor p = new CobtupdtProcessor(new ScriptedSql(Sqlca.NOT_FOUND));
        assertEquals(pad("No records found.", 80), p.process(InputRecord.of("U09x")).get(2));
        assertEquals(4, p.returnCode());
    }

    @Test
    void commentsAndBlankRecords() {
        CobtupdtProcessor p = new CobtupdtProcessor(new ScriptedSql());
        assertEquals("IGNORING COMMENTED LINE", p.process(InputRecord.of("*  skip")).get(1));
        assertEquals(0, p.returnCode());
        assertEquals(pad("ERROR: TYPE NOT VALID", 80), p.process(InputRecord.of("")).get(1));
        assertEquals(4, p.returnCode());
    }

    @Test
    void sqlcodeEditAndOcesqlMapping() {
        assertEquals(" -403", CobolText.minusMinus9(-403));
        assertEquals("    0", CobolText.minusMinus9(0));
        assertEquals("-2345", CobolText.minusMinus9(-12345));
        assertEquals(" 100", CobolText.minusMinus9(100).substring(1));
        assertEquals(-403, Sqlca.sqlcode(new SQLException("dup", "23505")));
        assertEquals(-400, Sqlca.sqlcode(new SQLException("fk", "23503")));
        assertEquals(-400, Sqlca.sqlcode(new SQLException("aborted", "25P02")));
        assertEquals(-9999, Sqlca.sqlcode(new SQLException("none")));
    }

    @Test
    void hostVariablesAreDecodedAsOcesqlSjisClientEncoding() {
        // "Ün" in UTF-8 is C3 9C 6E; the legacy row (probed) holds U+FF83 (C3) and U+5F9E (SJIS 9C 6E)
        assertEquals(java.util.Optional.of("\uff83\u5f9e"), Db2Attach.decodeSjis("\u00c3\u009cn"));
        assertEquals(java.util.Optional.of("Purchase  "), Db2Attach.decodeSjis("Purchase  "));
        // left to the server, whose error aborts the unit of work
        assertEquals(java.util.Optional.empty(), Db2Attach.decodeSjis("bad\u0081 "));
    }

    @Test
    void inputIsSplitAndPaddedLikeRecordPy() {
        assertEquals(List.of("A04x", "", "D03"), InpfileReader.splitlines("A04x\r\n\nD03"));
        assertEquals(List.of("a"), InpfileReader.splitlines("a\n"));
    }
}
