package com.carddemo.interest.record;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.batch.InterestCalculator;
import com.carddemo.interest.batch.Sysout;

class TranCatBalRecordTest {

    @Test
    void sysoutDisplaysOriginalPlainDigitEncoding() {
        String raw = Fixed.pad("0000000200101000100000100000", TranCatBalRecord.LENGTH);
        TranCatBalRecord balance = TranCatBalRecord.parse(raw);
        assertEquals(new BigDecimal("1000.00"), balance.tranCatBal());
        assertEquals(raw, balance.format());
        Sysout sysout = new Sysout();
        InterestCalculator calculator = new InterestCalculator(mock(JdbcTemplate.class), sysout,
                "2022071800", LocalDateTime::now);
        calculator.process(balance);
        assertEquals(raw, sysout.lines().get(0));
    }
}
