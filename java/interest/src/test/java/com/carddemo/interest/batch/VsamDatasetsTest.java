package com.carddemo.interest.batch;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.carddemo.interest.record.AccountRecord;
import com.carddemo.interest.record.Fixed;

class VsamDatasetsTest {

    @TempDir
    Path directory;

    @Test
    void unloadPreservesUntouchedBytesAndOnlyEncodesRewrittenFields() throws Exception {
        String original = Fixed.pad("00000002001Y" + "000000100000".repeat(3)
                + "2019-03-152026-12-312026-12-31" + "000000000000".repeat(2)
                + "10115     GOLD", AccountRecord.LENGTH);
        ResultSet untouched = mock(ResultSet.class);
        when(untouched.getString("raw_record")).thenReturn(original);
        ResultSet rewritten = mock(ResultSet.class);
        when(rewritten.getString("raw_record")).thenReturn(original);
        when(rewritten.getBoolean("rewritten")).thenReturn(true);
        when(rewritten.getBigDecimal("curr_bal")).thenReturn(new BigDecimal("1010.00"));
        when(rewritten.getBigDecimal("curr_cyc_credit")).thenReturn(new BigDecimal("0.00"));
        when(rewritten.getBigDecimal("curr_cyc_debit")).thenReturn(new BigDecimal("0.00"));
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(anyString(), ArgumentMatchers.<RowMapper<String>>any())).thenAnswer(invocation -> {
            RowMapper<String> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(untouched, 0), mapper.mapRow(rewritten, 1));
        });
        Path output = directory.resolve("ACCTFILE");
        new VsamDatasets(jdbc).unloadAccounts(output);
        String expectedRewrite = original.substring(0, 12) + "00000010100{"
                + original.substring(24, 78) + "00000000000{".repeat(2) + original.substring(102);
        assertArrayEquals((original + expectedRewrite).getBytes(StandardCharsets.ISO_8859_1),
                Files.readAllBytes(output));
    }
}
