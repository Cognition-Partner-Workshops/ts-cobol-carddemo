package com.carddemo.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void testDefaultConstructor() {
        Transaction transaction = new Transaction();
        assertNull(transaction.getTransactionId());
        assertNull(transaction.getTypeCode());
        assertNull(transaction.getAmount());
    }

    @Test
    void testParameterizedConstructor() {
        Transaction transaction = new Transaction(
                "TXN0000000000001",
                "SA",
                1001,
                new BigDecimal("150.00"),
                "4111111111111111"
        );

        assertEquals("TXN0000000000001", transaction.getTransactionId());
        assertEquals("SA", transaction.getTypeCode());
        assertEquals(1001, transaction.getCategoryCode());
        assertEquals(new BigDecimal("150.00"), transaction.getAmount());
        assertEquals("4111111111111111", transaction.getCardNumber());
    }

    @Test
    void testSettersAndGetters() {
        Transaction transaction = new Transaction();

        LocalDateTime originTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        LocalDateTime processTime = LocalDateTime.of(2024, 1, 15, 10, 30, 5);

        transaction.setTransactionId("TXN0000000000002");
        transaction.setTypeCode("PU");
        transaction.setCategoryCode(2001);
        transaction.setSource("ONLINE");
        transaction.setDescription("Online purchase at Amazon");
        transaction.setAmount(new BigDecimal("99.99"));
        transaction.setMerchantId(123456789L);
        transaction.setMerchantName("Amazon");
        transaction.setMerchantCity("Seattle");
        transaction.setMerchantZip("98101");
        transaction.setCardNumber("5500000000000004");
        transaction.setOriginationTimestamp(originTime);
        transaction.setProcessingTimestamp(processTime);

        assertEquals("TXN0000000000002", transaction.getTransactionId());
        assertEquals("PU", transaction.getTypeCode());
        assertEquals(2001, transaction.getCategoryCode());
        assertEquals("ONLINE", transaction.getSource());
        assertEquals("Online purchase at Amazon", transaction.getDescription());
        assertEquals(new BigDecimal("99.99"), transaction.getAmount());
        assertEquals(123456789L, transaction.getMerchantId());
        assertEquals("Amazon", transaction.getMerchantName());
        assertEquals("Seattle", transaction.getMerchantCity());
        assertEquals("98101", transaction.getMerchantZip());
        assertEquals("5500000000000004", transaction.getCardNumber());
        assertEquals(originTime, transaction.getOriginationTimestamp());
        assertEquals(processTime, transaction.getProcessingTimestamp());
    }

    @Test
    void testToString() {
        Transaction transaction = new Transaction(
                "TXN0000000000001",
                "SA",
                1001,
                new BigDecimal("150.00"),
                "4111111111111111"
        );

        String toString = transaction.toString();
        assertTrue(toString.contains("TXN0000000000001"));
        assertTrue(toString.contains("SA"));
        assertTrue(toString.contains("150.00"));
        assertTrue(toString.contains("4111111111111111"));
    }
}
