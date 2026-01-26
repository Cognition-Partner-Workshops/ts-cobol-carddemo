package com.aws.carddemo.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId("TESTUSER");
        user.setUserFirstName("Test");
        user.setUserLastName("User");
        user.setUserPassword("encoded_password");
        user.setUserType("U");
        user.setEnabled(true);
    }

    @Nested
    @DisplayName("User Type Tests - CSUSR01Y Pattern")
    class UserTypeTests {

        @Test
        @DisplayName("isAdmin returns true for type A")
        void isAdmin_TypeA_ReturnsTrue() {
            user.setUserType("A");
            assertTrue(user.isAdmin());
        }

        @Test
        @DisplayName("isAdmin returns false for type U")
        void isAdmin_TypeU_ReturnsFalse() {
            user.setUserType("U");
            assertFalse(user.isAdmin());
        }

        @Test
        @DisplayName("isAdmin returns false for null type")
        void isAdmin_NullType_ReturnsFalse() {
            user.setUserType(null);
            assertFalse(user.isAdmin());
        }

        @Test
        @DisplayName("isRegularUser returns true for type U")
        void isRegularUser_TypeU_ReturnsTrue() {
            user.setUserType("U");
            assertTrue(user.isRegularUser());
        }

        @Test
        @DisplayName("isRegularUser returns false for type A")
        void isRegularUser_TypeA_ReturnsFalse() {
            user.setUserType("A");
            assertFalse(user.isRegularUser());
        }
    }

    @Nested
    @DisplayName("User Full Name Tests")
    class UserFullNameTests {

        @Test
        @DisplayName("getFullName returns combined first and last name")
        void getFullName_ReturnsFullName() {
            user.setUserFirstName("John");
            user.setUserLastName("Doe");

            assertEquals("John Doe", user.getFullName());
        }

        @Test
        @DisplayName("getFullName handles null first name")
        void getFullName_NullFirstName_ReturnsLastName() {
            user.setUserFirstName(null);
            user.setUserLastName("Doe");

            assertEquals("null Doe", user.getFullName());
        }
    }

    @Nested
    @DisplayName("Spring Security UserDetails Tests")
    class UserDetailsTests {

        @Test
        @DisplayName("getUsername returns userId")
        void getUsername_ReturnsUserId() {
            user.setUserId("ADMIN001");
            assertEquals("ADMIN001", user.getUsername());
        }

        @Test
        @DisplayName("getPassword returns userPassword")
        void getPassword_ReturnsUserPassword() {
            user.setUserPassword("secret123");
            assertEquals("secret123", user.getPassword());
        }

        @Test
        @DisplayName("Admin user has ROLE_ADMIN authority")
        void getAuthorities_AdminUser_HasAdminRole() {
            user.setUserType("A");

            Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

            assertTrue(authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        }

        @Test
        @DisplayName("Regular user has ROLE_USER authority")
        void getAuthorities_RegularUser_HasUserRole() {
            user.setUserType("U");

            Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

            assertTrue(authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        }

        @Test
        @DisplayName("isAccountNonExpired returns true")
        void isAccountNonExpired_ReturnsTrue() {
            assertTrue(user.isAccountNonExpired());
        }

        @Test
        @DisplayName("isAccountNonLocked returns true")
        void isAccountNonLocked_ReturnsTrue() {
            assertTrue(user.isAccountNonLocked());
        }

        @Test
        @DisplayName("isCredentialsNonExpired returns true")
        void isCredentialsNonExpired_ReturnsTrue() {
            assertTrue(user.isCredentialsNonExpired());
        }

        @Test
        @DisplayName("isEnabled returns enabled status")
        void isEnabled_ReturnsEnabledStatus() {
            user.setEnabled(true);
            assertTrue(user.isEnabled());

            user.setEnabled(false);
            assertFalse(user.isEnabled());
        }
    }
}
