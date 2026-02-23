package com.carddemo.api.service;

import com.carddemo.api.dto.LoginRequest;
import com.carddemo.api.dto.LoginResponse;
import com.carddemo.core.domain.UserSecurity;
import com.carddemo.core.exception.AuthenticationException;
import com.carddemo.core.repository.UserSecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for Authentication operations.
 * Replaces business logic from COSGN00C (Signon Screen).
 *
 * Key COBOL logic replaced:
 * - VSAM READ on USRSEC file to validate user credentials
 * - Password comparison (COBOL plain text → Java BCrypt)
 * - Session establishment (CICS COMMAREA → JWT token)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserSecurityRepository userSecurityRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse authenticate(LoginRequest request) {
        UserSecurity user = userSecurityRepository.findByUsrId(request.getUserId())
                .orElseThrow(() -> new AuthenticationException("Invalid user ID or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getUsrPassword())) {
            throw new AuthenticationException("Invalid user ID or password");
        }

        // TODO: Generate JWT token in Phase 3 implementation
        String token = "jwt-token-placeholder";

        return LoginResponse.builder()
                .token(token)
                .userId(user.getUsrId())
                .userType(user.getUsrType())
                .firstName(user.getUsrFirstName())
                .lastName(user.getUsrLastName())
                .build();
    }
}
