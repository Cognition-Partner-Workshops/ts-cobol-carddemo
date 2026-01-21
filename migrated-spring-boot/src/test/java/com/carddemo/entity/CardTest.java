package com.carddemo.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @Test
    void testDefaultConstructor() {
        Card card = new Card();
        assertNull(card.getCardNumber());
        assertNull(card.getAccountId());
        assertNull(card.getCustomerId());
    }

    @Test
    void testParameterizedConstructor() {
        Card card = new Card("4111111111111111", 12345678901L, 123456789L, "John Doe");

        assertEquals("4111111111111111", card.getCardNumber());
        assertEquals(12345678901L, card.getAccountId());
        assertEquals(123456789L, card.getCustomerId());
        assertEquals("John Doe", card.getCardholderName());
        assertEquals("Y", card.getActiveStatus());
    }

    @Test
    void testSettersAndGetters() {
        Card card = new Card();

        card.setCardNumber("5500000000000004");
        card.setAccountId(98765432101L);
        card.setCustomerId(987654321L);
        card.setCardholderName("Jane Smith");
        card.setExpirationDate(LocalDate.of(2025, 12, 31));
        card.setActiveStatus("N");

        assertEquals("5500000000000004", card.getCardNumber());
        assertEquals(98765432101L, card.getAccountId());
        assertEquals(987654321L, card.getCustomerId());
        assertEquals("Jane Smith", card.getCardholderName());
        assertEquals(LocalDate.of(2025, 12, 31), card.getExpirationDate());
        assertEquals("N", card.getActiveStatus());
    }

    @Test
    void testToString() {
        Card card = new Card("4111111111111111", 12345678901L, 123456789L, "John Doe");

        String toString = card.toString();
        assertTrue(toString.contains("4111111111111111"));
        assertTrue(toString.contains("12345678901"));
        assertTrue(toString.contains("123456789"));
        assertTrue(toString.contains("Y"));
    }
}
