package com.carddemo.config;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataLoaderTest {

    @Test
    void parseSignedDecimalPositiveZero() {
        assertEquals(new BigDecimal("0.00"), DataLoader.parseSignedDecimal("00000000000{"));
    }

    @Test
    void parseSignedDecimalPositiveValue() {
        BigDecimal result = DataLoader.parseSignedDecimal("000001940000{");
        assertEquals(new BigDecimal("194000.00"), result);
    }

    @Test
    void parseSignedDecimalWithOverpunchG() {
        BigDecimal result = DataLoader.parseSignedDecimal("0000005047G");
        assertEquals(new BigDecimal("504.77"), result);
    }

    @Test
    void parseSignedDecimalNegativeOverpunch() {
        BigDecimal result = DataLoader.parseSignedDecimal("0000009190}");
        assertEquals(new BigDecimal("-919.00"), result);
    }

    @Test
    void parseSignedDecimalBlank() {
        assertEquals(BigDecimal.ZERO, DataLoader.parseSignedDecimal(""));
        assertEquals(BigDecimal.ZERO, DataLoader.parseSignedDecimal("   "));
    }

    @Test
    void parseSignedDecimalWithLetterJ() {
        BigDecimal result = DataLoader.parseSignedDecimal("00150J");
        assertEquals(new BigDecimal("-15.01"), result);
    }

    @Test
    void parseSignedDecimalWithLetterI() {
        BigDecimal result = DataLoader.parseSignedDecimal("00150I");
        assertEquals(new BigDecimal("15.09"), result);
    }
}
