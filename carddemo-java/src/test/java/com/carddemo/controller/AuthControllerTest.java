package com.carddemo.controller;

import com.carddemo.dto.LoginRequest;
import com.carddemo.dto.LoginResponse;
import com.carddemo.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginSuccess() throws Exception {
        LoginResponse response = new LoginResponse("test-token", "ADMIN", "Admin", "User", "A");
        when(authService.authenticate(any(LoginRequest.class))).thenReturn(response);

        LoginRequest request = new LoginRequest();
        request.setUserId("admin");
        request.setPassword("admin");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.userId").value("ADMIN"))
                .andExpect(jsonPath("$.userType").value("A"));
    }

    @Test
    void loginBadRequest() throws Exception {
        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Wrong Password. Try again ..."));

        LoginRequest request = new LoginRequest();
        request.setUserId("admin");
        request.setPassword("wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
