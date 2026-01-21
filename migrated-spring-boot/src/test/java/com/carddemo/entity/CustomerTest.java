package com.carddemo.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CustomerTest {

    @Test
    void testDefaultConstructor() {
        Customer customer = new Customer();
        assertNull(customer.getCustomerId());
        assertNull(customer.getFirstName());
        assertNull(customer.getLastName());
    }

    @Test
    void testParameterizedConstructor() {
        Customer customer = new Customer(123456789L, "John", "Doe");

        assertEquals(123456789L, customer.getCustomerId());
        assertEquals("John", customer.getFirstName());
        assertEquals("Doe", customer.getLastName());
    }

    @Test
    void testSettersAndGetters() {
        Customer customer = new Customer();

        customer.setCustomerId(123456789L);
        customer.setFirstName("Jane");
        customer.setMiddleName("Marie");
        customer.setLastName("Smith");
        customer.setAddressLine1("123 Main St");
        customer.setAddressLine2("Apt 4B");
        customer.setAddressLine3("Building C");
        customer.setStateCode("NY");
        customer.setCountryCode("USA");
        customer.setZipCode("10001");
        customer.setPhoneNumber1("555-123-4567");
        customer.setPhoneNumber2("555-987-6543");
        customer.setSsn(123456789L);
        customer.setGovernmentIssuedId("DL12345678");
        customer.setDateOfBirth(LocalDate.of(1985, 6, 15));
        customer.setEftAccountId("EFT001");
        customer.setPrimaryCardHolderIndicator("Y");
        customer.setFicoCreditScore(750);

        assertEquals(123456789L, customer.getCustomerId());
        assertEquals("Jane", customer.getFirstName());
        assertEquals("Marie", customer.getMiddleName());
        assertEquals("Smith", customer.getLastName());
        assertEquals("123 Main St", customer.getAddressLine1());
        assertEquals("Apt 4B", customer.getAddressLine2());
        assertEquals("Building C", customer.getAddressLine3());
        assertEquals("NY", customer.getStateCode());
        assertEquals("USA", customer.getCountryCode());
        assertEquals("10001", customer.getZipCode());
        assertEquals("555-123-4567", customer.getPhoneNumber1());
        assertEquals("555-987-6543", customer.getPhoneNumber2());
        assertEquals(123456789L, customer.getSsn());
        assertEquals("DL12345678", customer.getGovernmentIssuedId());
        assertEquals(LocalDate.of(1985, 6, 15), customer.getDateOfBirth());
        assertEquals("EFT001", customer.getEftAccountId());
        assertEquals("Y", customer.getPrimaryCardHolderIndicator());
        assertEquals(750, customer.getFicoCreditScore());
    }

    @Test
    void testToString() {
        Customer customer = new Customer(123456789L, "John", "Doe");
        customer.setStateCode("CA");

        String toString = customer.toString();
        assertTrue(toString.contains("123456789"));
        assertTrue(toString.contains("John"));
        assertTrue(toString.contains("Doe"));
        assertTrue(toString.contains("CA"));
    }
}
