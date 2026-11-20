package org.carddemo.modern.trantype;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UpdateProgramTest {
    @TempDir Path directory;
    private Connection connection;
    private PreparedStatement statement;
    private ArrayList<String> output;
    private UpdateProgram program;

    @BeforeEach
    void setup() throws Exception {
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        output = new ArrayList<>();
        program = new UpdateProgram(output::add);
    }

    @Test
    void insertsAndUpdatesPaddedDescriptions() throws Exception {
        program.process(record("A04Authorization"), connection);
        verify(statement).setString(1, "Authorization" + " ".repeat(37));
        verify(statement).setString(2, "04");
        program.process(record("U04Changed"), connection);
        verify(statement).setString(1, "Changed" + " ".repeat(43));
        assertThat(output).contains("RECORD INSERTED SUCCESSFULLY", "RECORD UPDATED SUCCESSFULLY");
        assertThat(program.returnCode()).isZero();
    }

    @Test
    void missingDeleteAndUpdateStillSucceed() {
        program.process(record("D99"), connection);
        program.process(record("U99Missing"), connection);
        assertThat(output).contains("RECORD DELETED SUCCESSFULLY", "RECORD UPDATED SUCCESSFULLY");
        assertThat(program.returnCode()).isZero();
    }

    @Test
    void commentAndInvalidTypeDoNotExecuteSql() throws Exception {
        program.process(record("*  comment"), connection);
        program.process(record("X99invalid"), connection);
        verifyNoInteractions(connection);
        assertThat(output).contains("IGNORING COMMENTED LINE", "ERROR: TYPE NOT VALID" + " ".repeat(59));
        assertThat(program.returnCode()).isEqualTo(4);
    }

    @Test
    void duplicateReportsOcesqlCodeAndContinuesWithStickyReturnCode() throws Exception {
        when(statement.executeUpdate()).thenThrow(new SQLException("duplicate", "23505")).thenReturn(1);
        program.process(record("A01duplicate"), connection);
        program.process(record("D02"), connection);
        assertThat(output).contains("Error accessing: TRANSACTION_TYPE table. SQLCODE: -403" + " ".repeat(26),
                "RECORD DELETED SUCCESSFULLY");
        assertThat(program.returnCode()).isEqualTo(4);
    }

    @Test
    void genericSqlErrorIsMinus400() throws Exception {
        when(statement.executeUpdate()).thenThrow(new SQLException("foreign key", "23503"));
        program.process(record("D01"), connection);
        assertThat(output.getLast()).startsWith("Error accessing: TRANSACTION_TYPE table. SQLCODE: -400");
        assertThat(program.returnCode()).isEqualTo(4);
    }

    @Test
    void emptyFileCommitsWithOnlyAttachOpenAndCommitOutput() throws Exception {
        Path input = Files.createFile(directory.resolve("empty"));
        program.run(input, connection);
        assertThat(output).containsExactly("DSNRUN: DSN ATTACHED, RUNNING COBTUPDT", "OPEN FILE OK",
                "DSNRUN: COMMIT AT END OF TASK");
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection, never()).prepareStatement(anyString());
        assertThat(program.returnCode()).isZero();
    }

    @Test
    void readsFixedRecordsNotLines() throws Exception {
        Path input = directory.resolve("input");
        Files.writeString(input, record("*  first") + record("*  second"));
        program.run(input, connection);
        assertThat(output.stream().filter("IGNORING COMMENTED LINE"::equals).count()).isEqualTo(2);
    }

    private static String record(String text) {
        return (text + " ".repeat(53)).substring(0, 53);
    }
}
