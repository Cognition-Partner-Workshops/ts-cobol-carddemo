package com.carddemo.service;

import com.carddemo.dto.LoginRequest;
import com.carddemo.dto.LoginResponse;
import com.carddemo.entity.UserSecurity;
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserSecurityRepository userSecurityRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private UserSecurity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserSecurity();
        testUser.setUsrId("ADMIN");
        testUser.setUsrFname("Admin");
        testUser.setUsrLname("User");
        testUser.setUsrPwd("ADMIN");
        testUser.setUsrType("A");
    }

    @Test
    void authenticateSuccess() {
        when(userSecurityRepository.findById("ADMIN")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(anyString(), anyString())).thenReturn("test-token");

        LoginRequest request = new LoginRequest();
        request.setUserId("admin");
        request.setPassword("admin");

        LoginResponse response = authService.authenticate(request);

        assertNotNull(response);
        assertEquals("test-token", response.getToken());
        assertEquals("ADMIN", response.getUserId());
        assertEquals("A", response.getUserType());
    }

    @Test
    void authenticateUserNotFound() {
        when(userSecurityRepository.findById("BADUSER")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setUserId("baduser");
        request.setPassword("pass");

        assertThrows(IllegalArgumentException.class, () -> authService.authenticate(request));
    }

    @Test
    void authenticateWrongPassword() {
        when(userSecurityRepository.findById("ADMIN")).thenReturn(Optional.of(testUser));

        LoginRequest request = new LoginRequest();
        request.setUserId("admin");
        request.setPassword("wrong");

        assertThrows(IllegalArgumentException.class, () -> authService.authenticate(request));
    }
}
