package com.carddemo.modern.dynamic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.carddemo.modern.codec.Layouts;
import org.junit.jupiter.api.Test;

class LegacyQuirksTest {
    @Test
    void column72AndCommentQuirk() {
        assertEquals("SELECT 1" + " ".repeat(65), RunUnit.assemble(List.of("-- comment", "SELECT 1")));
        assertEquals("X".repeat(72) + " ", RunUnit.assemble(List.of("X".repeat(80))));
        assertEquals("", RunUnit.assemble(List.of()));
        assertEquals("", RunUnit.assemble(List.of("--only comment")));
        assertEquals(800, RunUnit.assemble(java.util.Collections.nCopies(20, "X")).length());
    }

    @Test
    void sqlcodeFormattingAndDuplicateQuirk() {
        assertEquals("+0000000000", RunUnit.formatCode(0));
        assertEquals("-0000000400", RunUnit.formatCode(-400));
        assertEquals(-403, RunUnit.sqlcode(new SQLException("duplicate", "23505")));
        assertEquals(-400, RunUnit.sqlcode(new SQLException("aborted", "25P02")));
    }

    @Test
    void paddedVarcharQuirk() throws Exception {
        RunUnit unit = unit();
        var statement = mock(PreparedStatement.class);
        when(unit.connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        var record = Layouts.TRANTYPE_INPUT.decode("A04Authorization");
        new Cobtupdt(unit).write(record);
        verify(statement).setString(2, "Authorization" + " ".repeat(37));
        assertEquals(List.of("ADDING RECORD", "RECORD INSERTED SUCCESSFULLY"), unit.sysout);
    }

    @Test
    void failedTransactionAndRcOverwriteQuirk() throws Exception {
        RunUnit unit = unit();
        unit.rc = 8;
        when(unit.connection.prepareStatement(anyString())).thenThrow(new SQLException("aborted", "25P02"));
        new Cobtupdt(unit).write(Layouts.TRANTYPE_INPUT.decode("U01Purchase"));
        assertEquals(4, unit.rc);
        assertEquals(RunUnit.pad("Error accessing: TRANSACTION_TYPE table. SQLCODE: -400", 80),
                unit.sysout.getLast());
    }

    @Test
    void missingDeleteAndUpdateAreSuccessfulQuirk() throws Exception {
        RunUnit unit = unit();
        var statement = mock(PreparedStatement.class);
        when(unit.connection.prepareStatement(anyString())).thenReturn(statement);
        var program = new Cobtupdt(unit);
        program.write(Layouts.TRANTYPE_INPUT.decode("D99"));
        program.write(Layouts.TRANTYPE_INPUT.decode("U99Missing"));
        assertEquals(0, unit.rc);
        assertTrue(unit.sysout.contains("RECORD DELETED SUCCESSFULLY"));
        assertTrue(unit.sysout.contains("RECORD UPDATED SUCCESSFULLY"));
    }

    @Test
    void commentsAndInvalidTypesContinueQuirk() throws Exception {
        RunUnit unit = unit();
        var program = new Cobtupdt(unit);
        program.write(Layouts.TRANTYPE_INPUT.decode("* comment"));
        program.write(Layouts.TRANTYPE_INPUT.decode("Z"));
        assertEquals(4, unit.rc);
        assertEquals(RunUnit.pad("ERROR: TYPE NOT VALID", 80), unit.sysout.getLast());
        verifyNoInteractions(unit.connection);
    }

    @Test
    void emptyDynamicStatementQuirk(@org.junit.jupiter.api.io.TempDir java.nio.file.Path fixture)
            throws Exception {
        RunUnit unit = unit();
        unit.caseDir = fixture;
        unit.program = "COBTUPDT";
        unit.dynamicSql();
        assertEquals(8, unit.rc);
        assertTrue(unit.sysout.contains("CBTRNDYN: EXECUTE SQLCODE -0000000212"));
        verifyNoInteractions(unit.connection);
    }

    @Test
    void stopRunSuppressesDriverEndAndCommitsAtEndOfTaskQuirk(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path out) throws Exception {
        RunUnit unit = unit();
        unit.caseDir = out;
        unit.program = "COBTUPDT";
        java.nio.file.Files.createDirectories(out.resolve("input"));
        java.nio.file.Files.writeString(out.resolve("input/DYNSQL.txt"), "DELETE FROM TRANSACTION_TYPE");
        var statement = mock(PreparedStatement.class);
        when(unit.connection.prepareStatement(anyString())).thenReturn(statement);
        unit.dynamicSql();
        verify(unit.connection, never()).commit();
        unit.finish();
        verify(unit.connection).commit();
        assertEquals("DSNRUN: COMMIT AT END OF TASK", unit.sysout.getLast());
        assertFalse(unit.sysout.contains("CBTRNDYN: END"));
    }

    private static RunUnit unit() {
        RunUnit unit = new RunUnit();
        unit.connection = mock(Connection.class);
        return unit;
    }
}
