package com.aws.carddemo.authorization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.aws.carddemo.user.AppUser;
import com.aws.carddemo.user.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        appUserRepository.findByUserId("ADMIN001").ifPresent(u -> appUserRepository.deleteById(u.getId()));
        AppUser admin = new AppUser();
        admin.setUserId("ADMIN001");
        admin.setPassword(passwordEncoder.encode("PASSWORD"));
        admin.setFirstName("System");
        admin.setLastName("Admin");
        admin.setUserType("A");
        admin.setEnabled(true);
        appUserRepository.save(admin);
    }

    @Test
    void loginSuccessReturnsToken() throws Exception {
        var payload = new AuthController.LoginRequest("ADMIN001", "PASSWORD");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.userId").value("ADMIN001"))
                .andExpect(jsonPath("$.userType").value("A"));
    }

    @Test
    void loginFailureWithBadPassword() throws Exception {
        var payload = new AuthController.LoginRequest("ADMIN001", "WRONGPWD");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }
}
