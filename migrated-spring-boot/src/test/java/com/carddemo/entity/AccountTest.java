package com.carddemo.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void testDefaultConstructor() {
        Account account = new Account();
        assertNull(account.getAccountId());
        assertNull(account.getActiveStatus());
        assertNull(account.getCurrentBalance());
    }

    @Test
    void testParameterizedConstructor() {
        Account account = new Account(
                12345678901L,
                "Y",
                new BigDecimal("1000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("1000.00")
        );

        assertEquals(12345678901L, account.getAccountId());
        assertEquals("Y", account.getActiveStatus());
        assertEquals(new BigDecimal("1000.00"), account.getCurrentBalance());
        assertEquals(new BigDecimal("5000.00"), account.getCreditLimit());
        assertEquals(new BigDecimal("1000.00"), account.getCashCreditLimit());
        assertEquals(BigDecimal.ZERO, account.getCurrentCycleCredit());
        assertEquals(BigDecimal.ZERO, account.getCurrentCycleDebit());
    }

    @Test
    void testSettersAndGetters() {
        Account account = new Account();

        account.setAccountId(12345678901L);
        account.setActiveStatus("Y");
        account.setCurrentBalance(new BigDecimal("2500.50"));
        account.setCreditLimit(new BigDecimal("10000.00"));
        account.setCashCreditLimit(new BigDecimal("2000.00"));
        account.setOpenDate(LocalDate.of(2020, 1, 15));
        account.setExpirationDate(LocalDate.of(2025, 1, 15));
        account.setReissueDate(LocalDate.of(2023, 1, 15));
        account.setCurrentCycleCredit(new BigDecimal("500.00"));
        account.setCurrentCycleDebit(new BigDecimal("300.00"));
        account.setAddressZip("12345");
        account.setGroupId("GRP001");

        assertEquals(12345678901L, account.getAccountId());
        assertEquals("Y", account.getActiveStatus());
        assertEquals(new BigDecimal("2500.50"), account.getCurrentBalance());
        assertEquals(new BigDecimal("10000.00"), account.getCreditLimit());
        assertEquals(new BigDecimal("2000.00"), account.getCashCreditLimit());
        assertEquals(LocalDate.of(2020, 1, 15), account.getOpenDate());
        assertEquals(LocalDate.of(2025, 1, 15), account.getExpirationDate());
        assertEquals(LocalDate.of(2023, 1, 15), account.getReissueDate());
        assertEquals(new BigDecimal("500.00"), account.getCurrentCycleCredit());
        assertEquals(new BigDecimal("300.00"), account.getCurrentCycleDebit());
        assertEquals("12345", account.getAddressZip());
        assertEquals("GRP001", account.getGroupId());
    }

    @Test
    void testToString() {
        Account account = new Account(
                12345678901L,
                "Y",
                new BigDecimal("1000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("1000.00")
        );

        String toString = account.toString();
        assertTrue(toString.contains("12345678901"));
        assertTrue(toString.contains("Y"));
        assertTrue(toString.contains("1000.00"));
        assertTrue(toString.contains("5000.00"));
    }
}
