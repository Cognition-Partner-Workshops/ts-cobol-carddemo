package com.carddemo.controller;

import com.carddemo.dto.LoginRequest;
import com.carddemo.dto.LoginResponse;
import com.carddemo.entity.User;
import com.carddemo.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @Test
    void login_validCredentials_returnsSuccess() {
        User user = new User();
        user.setUserId("USER0001");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setUserType("R");

        when(userService.authenticate("USER0001", "pass1234")).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setUserId("USER0001");
        request.setPassword("pass1234");

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertNotNull(response.getBody());
        assertEquals("USER0001", response.getBody().getUserId());
        assertEquals("Login successful", response.getBody().getMessage());
        assertEquals("John", response.getBody().getFirstName());
    }
}
