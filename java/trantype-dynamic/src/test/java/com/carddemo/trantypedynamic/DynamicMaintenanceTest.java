package com.carddemo.trantypedynamic;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DynamicMaintenanceTest {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final DynamicMaintenance maintenance = new DynamicMaintenance(new PrintStream(bytes));

    @Test
    void assemblesFixedColumnsAndSkipsOnlyColumnOneComments() {
        var input = DynamicMaintenance.assemble(List.of("-- comment", "UPDATE t", " ".repeat(72) + "ignored"));
        assertThat(input.lines()).isEqualTo(2);
        assertThat(input.statement()).isEqualTo(DynamicMaintenance.fixed("UPDATE t", 72) + " " + " ".repeat(73));
        assertThat(DynamicMaintenance.assemble(List.of("x".repeat(80))).statement()).isEqualTo("x".repeat(72) + " ");
    }

    @Test
    void emptyAndOverflowSqlBuffer() {
        assertThat(DynamicMaintenance.assemble(List.of())).isEqualTo(new DynamicMaintenance.SqlInput("", 0));
        assertThat(DynamicMaintenance.assemble(java.util.Collections.nCopies(100, "x")).lines()).isZero();
        assertThat(DynamicMaintenance.assemble(java.util.Collections.nCopies(12, "x")).statement()).hasSize(800);
    }

    @Test
    void fixedRecordCodecKeepsSpacesAndRejectsPartialRecord() {
        assertThat(DynamicApplication.records("A01abc  D02     ".getBytes(StandardCharsets.ISO_8859_1), 8))
                .containsExactly("A01abc  ", "D02     ");
        assertThat(DynamicApplication.records(new byte[0], 53)).isEmpty();
        assertThatThrownBy(() -> DynamicApplication.records(new byte[1], 53))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dynamicSqlAndCalledUpdatesShareConnectionWithPaddedDescription() throws Exception {
        Connection connection = mock(Connection.class);
        Statement dynamic = mock(Statement.class);
        PreparedStatement update = mock(PreparedStatement.class);
        when(connection.createStatement()).thenReturn(dynamic);
        when(connection.prepareStatement(anyString())).thenReturn(update);
        int rc = maintenance.run(connection, DynamicMaintenance.assemble(List.of("DELETE FROM t")),
                "COBTUPDT", List.of("A04Authorization", "U01Purchase", "D03", "*ignored"));
        assertThat(rc).isZero();
        verify(dynamic).execute(anyString());
        verify(update).setString(1, DynamicMaintenance.fixed("Authorization", 50));
        verify(update).setString(2, "04");
        verify(update).setString(1, "03");
        verify(connection).commit();
        assertThat(bytes.toString()).contains("RECORD INSERTED SUCCESSFULLY", "RECORD UPDATED SUCCESSFULLY",
                "RECORD DELETED SUCCESSFULLY", "IGNORING COMMENTED LINE").doesNotContain("CBTRNDYN: END");
    }

    @Test
    void failedDynamicSqlStillCallsWithoutRollbackAndCalledReturnCodeWins() throws Exception {
        Connection connection = mock(Connection.class);
        Statement dynamic = mock(Statement.class);
        PreparedStatement update = mock(PreparedStatement.class);
        when(connection.createStatement()).thenReturn(dynamic);
        when(dynamic.execute(anyString())).thenThrow(new SQLException("bad column", "42703"));
        when(connection.prepareStatement(anyString())).thenReturn(update);
        when(update.executeUpdate()).thenThrow(new SQLException("transaction aborted", "25P02"));
        assertThat(maintenance.run(connection, new DynamicMaintenance.SqlInput("bad", 1),
                "COBTUPDT", List.of("U01Purchase"))).isEqualTo(4);
        verify(connection, never()).rollback();
        verify(connection).commit();
        assertThat(bytes.toString()).contains("EXECUTE SQLCODE -0000000400",
                DynamicMaintenance.fixed("Error accessing: TRANSACTION_TYPE table. SQLCODE: -400", 80));
    }

    @Test
    void emptyCalledInputClearsDynamicFailureReturnCode() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(mock(Statement.class));
        assertThat(maintenance.run(connection, new DynamicMaintenance.SqlInput("", 0), "COBTUPDT", List.of())).isZero();
        assertThat(bytes.toString()).contains("EXECUTE SQLCODE -0000000212", "OPEN FILE OK");
    }

    @Test
    void noCallReturnsDynamicFailureAndPrintsEnd() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenThrow(new SQLException("syntax", "42601"));
        assertThat(maintenance.run(connection, new DynamicMaintenance.SqlInput("bad", 1), "", List.of())).isEqualTo(8);
        assertThat(bytes.toString()).contains("EXECUTE SQLCODE -0000000400", "CBTRNDYN: END");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "; ;", "--comment", "/* comment */", "/* outer /* nested */ */ ;"})
    void emptyQueryReportsMinus212AndNoParmReturnsEight(String sql) throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(mock(Statement.class));
        assertThat(maintenance.run(connection, new DynamicMaintenance.SqlInput(sql, 0), "", List.of())).isEqualTo(8);
        assertThat(bytes.toString()).contains("EXECUTE SQLCODE -0000000212", "CBTRNDYN: END");
        verify(connection).commit();
    }

    @Test
    void sqlTokenDetectionDoesNotConfuseQuotedTextOrInvalidCommentWithEmptySql() {
        assertThat(DynamicMaintenance.hasSqlToken("SELECT '--'")).isTrue();
        assertThat(DynamicMaintenance.hasSqlToken("--comment\nSELECT 1")).isTrue();
        assertThat(DynamicMaintenance.hasSqlToken("/* unterminated")).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"true,+0000000000", "false,+0000000100"})
    void selectResultEmptinessSetsSqlcodeButNotErrorReturnCode(boolean hasRow, String code) throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet result = mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);
        when(statement.getResultSet()).thenReturn(result);
        when(result.next()).thenReturn(hasRow);
        assertThat(maintenance.run(connection, new DynamicMaintenance.SqlInput("SELECT 1", 1), "", List.of())).isZero();
        assertThat(bytes.toString()).contains("EXECUTE SQLCODE " + code, "CBTRNDYN: END");
        verify(result).close();
    }

    @ParameterizedTest
    @CsvSource({"08001,-402", "08003,-220", "08007,-401", "21000,-404", "23505,-403",
            "25001,-603", "25P01,-604", "34000,-602", "42804,-204", "42P03,-605",
            "55P03,-400", "25P02,-400", "42601,-400", "42703,-400"})
    void serverSqlstatesMatchOcesqlMapping(String state, int code) throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenThrow(new SQLException("failure", state));
        assertThat(DynamicMaintenance.sqlcode(new SQLException("failure", state))).isEqualTo(code);
        assertThat(maintenance.run(connection, new DynamicMaintenance.SqlInput("sql", 1), "", List.of())).isEqualTo(8);
        assertThat(bytes.toString()).contains(String.format("EXECUTE SQLCODE %+011d", code));
    }

    @Test
    void absentServerSqlstateMapsToUndefinedOcesqlError() {
        assertThat(DynamicMaintenance.sqlcode(new SQLException("no SQLSTATE"))).isEqualTo(-9999);
    }

    @Test
    void invalidOperationAndDuplicateAreReportedAndProcessingContinues() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(mock(Statement.class));
        PreparedStatement update = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(update);
        when(update.executeUpdate()).thenThrow(new SQLException("duplicate", "23505"));
        assertThat(maintenance.run(connection, new DynamicMaintenance.SqlInput("sql", 1),
                "COBTUPDT", List.of("?01", "A01", "*comment"))).isEqualTo(4);
        assertThat(bytes.toString()).contains("ERROR: TYPE NOT VALID", "SQLCODE: -403", "IGNORING COMMENTED LINE");
    }

    @Test
    void unsupportedCallFailsExplicitly() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(mock(Statement.class));
        assertThatThrownBy(() -> maintenance.run(connection, new DynamicMaintenance.SqlInput("sql", 1),
                "UNKNOWN", List.of())).isInstanceOf(IllegalArgumentException.class);
        verify(connection, never()).commit();
    }

    @ParameterizedTest
    @CsvSource({"23505,-403", "25P02,-400"})
    void invalidOperationRetainsSqlErrorTailAcrossComments(String state, int code) throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(mock(Statement.class));
        PreparedStatement update = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(update);
        when(update.executeUpdate()).thenThrow(new SQLException("failure", state));
        assertThat(maintenance.run(connection, new DynamicMaintenance.SqlInput("sql", 1),
                "COBTUPDT", List.of("A01", "*comment", "?01", "?02"))).isEqualTo(4);
        String retained = DynamicMaintenance.fixed("ERROR: TYPE NOT VALIDSACTION_TYPE table. SQLCODE:"
                + String.format("%5d", code), 80);
        assertThat(bytes.toString().lines().filter(line -> line.startsWith("ERROR: TYPE")))
                .containsExactly(retained, retained);
    }

    @Test
    void invalidOperationStartsWithBlankBufferOnEachInvocation() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(mock(Statement.class));
        PreparedStatement update = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(update);
        when(update.executeUpdate()).thenThrow(new SQLException("duplicate", "23505"));
        maintenance.run(connection, new DynamicMaintenance.SqlInput("sql", 1), "COBTUPDT", List.of("A01"));
        bytes.reset();
        assertThat(maintenance.run(connection, new DynamicMaintenance.SqlInput("sql", 1),
                "COBTUPDT", List.of("?01"))).isEqualTo(4);
        assertThat(bytes.toString().lines().filter(line -> line.startsWith("ERROR: TYPE")))
                .containsExactly(DynamicMaintenance.fixed("ERROR: TYPE NOT VALID", 80));
    }
}
