package com.aws.carddemo.service;

import com.aws.carddemo.dto.LoginRequest;
import com.aws.carddemo.dto.LoginResponse;
import com.aws.carddemo.entity.User;
import com.aws.carddemo.repository.UserRepository;
import com.aws.carddemo.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setUserId("ADMIN001");
        adminUser.setUserFirstName("Admin");
        adminUser.setUserLastName("User");
        adminUser.setUserPassword("encoded_password");
        adminUser.setUserType("A");
        adminUser.setEnabled(true);

        regularUser = new User();
        regularUser.setUserId("USER0001");
        regularUser.setUserFirstName("Regular");
        regularUser.setUserLastName("User");
        regularUser.setUserPassword("encoded_password");
        regularUser.setUserType("U");
        regularUser.setEnabled(true);
    }

    @Nested
    @DisplayName("Authentication Tests - COSGN00C Pattern")
    class AuthenticationTests {

        @Test
        @DisplayName("Successful admin user authentication returns correct response")
        void authenticate_AdminUser_ReturnsSuccessWithAdminType() {
            LoginRequest request = new LoginRequest("ADMIN001", "password");
            Authentication authentication = mock(Authentication.class);

            when(userRepository.findByUserId("ADMIN001")).thenReturn(Optional.of(adminUser));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(tokenProvider.generateToken(authentication)).thenReturn("jwt_token");

            LoginResponse response = authenticationService.authenticate(request);

            assertTrue(response.isSuccess());
            assertEquals("jwt_token", response.getToken());
            assertEquals("ADMIN001", response.getUserId());
            assertEquals("A", response.getUserType());
            assertEquals("Admin", response.getFirstName());
            assertEquals("User", response.getLastName());
        }

        @Test
        @DisplayName("Successful regular user authentication returns correct response")
        void authenticate_RegularUser_ReturnsSuccessWithUserType() {
            LoginRequest request = new LoginRequest("USER0001", "password");
            Authentication authentication = mock(Authentication.class);

            when(userRepository.findByUserId("USER0001")).thenReturn(Optional.of(regularUser));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(tokenProvider.generateToken(authentication)).thenReturn("jwt_token");

            LoginResponse response = authenticationService.authenticate(request);

            assertTrue(response.isSuccess());
            assertEquals("U", response.getUserType());
        }

        @Test
        @DisplayName("User ID is converted to uppercase for lookup")
        void authenticate_LowercaseUserId_ConvertsToUppercase() {
            LoginRequest request = new LoginRequest("admin001", "password");
            Authentication authentication = mock(Authentication.class);

            when(userRepository.findByUserId("ADMIN001")).thenReturn(Optional.of(adminUser));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(tokenProvider.generateToken(authentication)).thenReturn("jwt_token");

            LoginResponse response = authenticationService.authenticate(request);

            verify(userRepository).findByUserId("ADMIN001");
            assertTrue(response.isSuccess());
        }

        @Test
        @DisplayName("User not found throws UsernameNotFoundException")
        void authenticate_UserNotFound_ThrowsException() {
            LoginRequest request = new LoginRequest("UNKNOWN", "password");

            when(userRepository.findByUserId("UNKNOWN")).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class,
                    () -> authenticationService.authenticate(request));
        }

        @Test
        @DisplayName("Invalid password throws BadCredentialsException")
        void authenticate_InvalidPassword_ThrowsException() {
            LoginRequest request = new LoginRequest("ADMIN001", "wrong_password");

            when(userRepository.findByUserId("ADMIN001")).thenReturn(Optional.of(adminUser));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThrows(BadCredentialsException.class,
                    () -> authenticationService.authenticate(request));
        }
    }

    @Nested
    @DisplayName("Admin User Check Tests")
    class AdminUserCheckTests {

        @Test
        @DisplayName("isAdminUser returns true for admin user")
        void isAdminUser_AdminUser_ReturnsTrue() {
            when(userRepository.findByUserId("ADMIN001")).thenReturn(Optional.of(adminUser));

            assertTrue(authenticationService.isAdminUser("ADMIN001"));
        }

        @Test
        @DisplayName("isAdminUser returns false for regular user")
        void isAdminUser_RegularUser_ReturnsFalse() {
            when(userRepository.findByUserId("USER0001")).thenReturn(Optional.of(regularUser));

            assertFalse(authenticationService.isAdminUser("USER0001"));
        }

        @Test
        @DisplayName("isAdminUser returns false for non-existent user")
        void isAdminUser_NonExistentUser_ReturnsFalse() {
            when(userRepository.findByUserId("UNKNOWN")).thenReturn(Optional.empty());

            assertFalse(authenticationService.isAdminUser("UNKNOWN"));
        }

        @Test
        @DisplayName("isAdminUser converts userId to uppercase")
        void isAdminUser_LowercaseUserId_ConvertsToUppercase() {
            when(userRepository.findByUserId("ADMIN001")).thenReturn(Optional.of(adminUser));

            authenticationService.isAdminUser("admin001");

            verify(userRepository).findByUserId("ADMIN001");
        }
    }
}
