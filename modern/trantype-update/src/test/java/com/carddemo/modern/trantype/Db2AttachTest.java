package com.carddemo.modern.trantype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class Db2AttachTest {
    @Test
    void invalidSjisIsRejectedByServerAndLaterStatementsRemainAborted() throws Exception {
        var source = mock(DataSource.class);
        var connection = mock(Connection.class);
        var statement = mock(PreparedStatement.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1)
                .thenThrow(new SQLException("invalid SJIS", "22021"))
                .thenThrow(new SQLException("transaction aborted", "25P02"));
        var attach = new Db2Attach(source, mock(Sysout.class));
        attach.attach("COBTUPDT");
        assertEquals(0, attach.insert("81", "Before"));
        assertEquals(-400, attach.insert("82", "bad\u0081 "));
        assertEquals(-400, attach.insert("83", "After"));
        verify(statement).setBytes(2, new byte[]{'b', 'a', 'd', (byte) 0x81, ' '});
        verify(connection, times(3)).prepareStatement(
                "INSERT INTO carddemo.transaction_type (tr_type, tr_description) VALUES (convert_from(?, 'SJIS'), convert_from(?, 'SJIS'))");
        attach.commitAtEndOfTask();
        verify(connection).commit();
        verify(connection).close();
    }
}
