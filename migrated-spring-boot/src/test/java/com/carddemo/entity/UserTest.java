package com.carddemo.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testDefaultConstructor() {
        User user = new User();
        assertNull(user.getUserId());
        assertNull(user.getFirstName());
        assertNull(user.getLastName());
    }

    @Test
    void testParameterizedConstructor() {
        User user = new User("ADMIN001", "Admin", "User", "password", "A");

        assertEquals("ADMIN001", user.getUserId());
        assertEquals("Admin", user.getFirstName());
        assertEquals("User", user.getLastName());
        assertEquals("password", user.getPassword());
        assertEquals("A", user.getUserType());
    }

    @Test
    void testSettersAndGetters() {
        User user = new User();

        user.setUserId("USER0001");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPassword("secret");
        user.setUserType("U");

        assertEquals("USER0001", user.getUserId());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("secret", user.getPassword());
        assertEquals("U", user.getUserType());
    }

    @Test
    void testIsAdminTrue() {
        User adminUser = new User("ADMIN001", "Admin", "User", "password", "A");
        assertTrue(adminUser.isAdmin());
    }

    @Test
    void testIsAdminFalse() {
        User regularUser = new User("USER0001", "Regular", "User", "password", "U");
        assertFalse(regularUser.isAdmin());
    }

    @Test
    void testToString() {
        User user = new User("USER0001", "John", "Doe", "password", "U");

        String toString = user.toString();
        assertTrue(toString.contains("USER0001"));
        assertTrue(toString.contains("John"));
        assertTrue(toString.contains("Doe"));
        assertTrue(toString.contains("U"));
        assertFalse(toString.contains("password"));
    }
}
