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
        var record = Layouts.TRAN_TYPE_MAINT.decode("A04Authorization");
        new Cobtupdt(unit).write(record);
        verify(statement).setString(2, "Authorization" + " ".repeat(37));
        assertEquals(List.of("ADDING RECORD", "RECORD INSERTED SUCCESSFULLY"), unit.sysout);
    }

    @Test
    void failedTransactionAndRcOverwriteQuirk() throws Exception {
        RunUnit unit = unit();
        unit.rc = 8;
        when(unit.connection.prepareStatement(anyString())).thenThrow(new SQLException("aborted", "25P02"));
        new Cobtupdt(unit).write(Layouts.TRAN_TYPE_MAINT.decode("U01Purchase"));
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
        program.write(Layouts.TRAN_TYPE_MAINT.decode("D99"));
        program.write(Layouts.TRAN_TYPE_MAINT.decode("U99Missing"));
        assertEquals(0, unit.rc);
        assertTrue(unit.sysout.contains("RECORD DELETED SUCCESSFULLY"));
        assertTrue(unit.sysout.contains("RECORD UPDATED SUCCESSFULLY"));
    }

    @Test
    void successfulSqlCallResetsEarlierInvalidTypeReturnCodeQuirk() throws Exception {
        RunUnit unit = unit();
        var statement = mock(PreparedStatement.class);
        when(unit.connection.prepareStatement(anyString())).thenReturn(statement);
        var program = new Cobtupdt(unit);
        program.write(Layouts.TRAN_TYPE_MAINT.decode("X99Invalid"));
        assertEquals(4, unit.rc);
        program.write(Layouts.TRAN_TYPE_MAINT.decode("U99Missing"));
        assertEquals(0, unit.rc);
    }

    @Test
    void commentsAndInvalidTypesContinueQuirk() throws Exception {
        RunUnit unit = unit();
        var program = new Cobtupdt(unit);
        program.write(Layouts.TRAN_TYPE_MAINT.decode("* comment"));
        program.write(Layouts.TRAN_TYPE_MAINT.decode("Z"));
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

    @Test
    void unexpectedChunkFailureDoesNotCommitOrRecordSuccess(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path out) throws Exception {
        RunUnit unit = spy(unit());
        doNothing().when(unit).open(any(), any());
        var launcher = mock(org.springframework.batch.core.launch.JobLauncher.class);
        var job = mock(org.springframework.batch.core.Job.class);
        var execution = new org.springframework.batch.core.JobExecution(1L);
        execution.setStatus(org.springframework.batch.core.BatchStatus.FAILED);
        execution.addFailureException(new IllegalStateException("unexpected writer failure"));
        when(launcher.run(eq(job), any())).thenReturn(execution);
        var runner = new DynamicRunner(unit, launcher, job);
        var args = new org.springframework.boot.DefaultApplicationArguments(
                "--case=" + out, "--out=" + out.resolve("actual"), "--estate.root=" + out);
        assertThrows(IllegalStateException.class, () -> runner.run(args));
        verify(unit.connection, never()).commit();
        verify(unit.connection).close();
        assertFalse(java.nio.file.Files.exists(out.resolve("actual/rc.json")));
        assertFalse(unit.sysout.contains("DSNRUN: COMMIT AT END OF TASK"));
    }

    private static RunUnit unit() {
        RunUnit unit = new RunUnit();
        unit.connection = mock(Connection.class);
        return unit;
    }
}
