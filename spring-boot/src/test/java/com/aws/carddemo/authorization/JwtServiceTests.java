package com.aws.carddemo.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class JwtServiceTests {

    @Autowired
    private JwtService jwtService;

    @Test
    void generateAndValidateToken() {
        String token = jwtService.generateToken("ADMIN001", "A");
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo("ADMIN001");
        assertThat(jwtService.extractUserType(token)).isEqualTo("A");
    }
}
