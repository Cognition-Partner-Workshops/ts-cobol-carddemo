package com.carddemo.entity;

import com.carddemo.entity.TransactionCategoryBalance.TransactionCategoryBalanceId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransactionCategoryBalanceTest {

    @Test
    void testDefaultConstructor() {
        TransactionCategoryBalance tcb = new TransactionCategoryBalance();
        assertNull(tcb.getId());
        assertNull(tcb.getBalance());
    }

    @Test
    void testParameterizedConstructor() {
        TransactionCategoryBalance tcb = new TransactionCategoryBalance(
                12345678901L,
                "SA",
                1001,
                new BigDecimal("500.00")
        );

        assertNotNull(tcb.getId());
        assertEquals(12345678901L, tcb.getAccountId());
        assertEquals("SA", tcb.getTypeCode());
        assertEquals(1001, tcb.getCategoryCode());
        assertEquals(new BigDecimal("500.00"), tcb.getBalance());
    }

    @Test
    void testSettersAndGetters() {
        TransactionCategoryBalance tcb = new TransactionCategoryBalance();
        TransactionCategoryBalanceId id = new TransactionCategoryBalanceId(
                98765432101L, "PU", 2001
        );

        tcb.setId(id);
        tcb.setBalance(new BigDecimal("1250.75"));

        assertEquals(id, tcb.getId());
        assertEquals(98765432101L, tcb.getAccountId());
        assertEquals("PU", tcb.getTypeCode());
        assertEquals(2001, tcb.getCategoryCode());
        assertEquals(new BigDecimal("1250.75"), tcb.getBalance());
    }

    @Test
    void testIdEquals() {
        TransactionCategoryBalanceId id1 = new TransactionCategoryBalanceId(
                12345678901L, "SA", 1001
        );
        TransactionCategoryBalanceId id2 = new TransactionCategoryBalanceId(
                12345678901L, "SA", 1001
        );
        TransactionCategoryBalanceId id3 = new TransactionCategoryBalanceId(
                12345678901L, "SA", 1002
        );

        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testIdSettersAndGetters() {
        TransactionCategoryBalanceId id = new TransactionCategoryBalanceId();

        id.setAccountId(12345678901L);
        id.setTypeCode("CR");
        id.setCategoryCode(3001);

        assertEquals(12345678901L, id.getAccountId());
        assertEquals("CR", id.getTypeCode());
        assertEquals(3001, id.getCategoryCode());
    }

    @Test
    void testToString() {
        TransactionCategoryBalance tcb = new TransactionCategoryBalance(
                12345678901L,
                "SA",
                1001,
                new BigDecimal("500.00")
        );

        String toString = tcb.toString();
        assertTrue(toString.contains("500.00"));
    }

    @Test
    void testIdToString() {
        TransactionCategoryBalanceId id = new TransactionCategoryBalanceId(
                12345678901L, "SA", 1001
        );

        String toString = id.toString();
        assertTrue(toString.contains("12345678901"));
        assertTrue(toString.contains("SA"));
        assertTrue(toString.contains("1001"));
    }
}
