package com.cardemo.service;

import com.cardemo.dto.LoginRequest;
import com.cardemo.dto.LoginResponse;
import com.cardemo.entity.CardDemoUser;
import com.cardemo.exception.CardDemoException;
import com.cardemo.repository.CardDemoUserRepository;
import com.cardemo.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

/**
 * Authentication service.
 * Migrated from COSGN00C (CC00 transaction) - Signon Screen logic.
 * COBOL flow: PROCESS-ENTER-KEY -> READ-USER-SEC-FILE -> password comparison
 */
@Service
public class AuthService {

    private final CardDemoUserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    public AuthService(CardDemoUserRepository userRepository, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Authenticate user - migrated from COSGN00C READ-USER-SEC-FILE paragraph.
     * COBOL: EXEC CICS READ DATASET(WS-USRSEC-FILE) INTO(SEC-USER-DATA)
     *        IF SEC-USR-PWD = WS-USER-PWD -> success
     *        WHEN 13 -> User not found
     */
    public LoginResponse login(LoginRequest request) {
        // Validate input - migrated from PROCESS-ENTER-KEY
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw CardDemoException.badRequest("Please enter User ID ...");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw CardDemoException.badRequest("Please enter Password ...");
        }

        String userId = request.getUserId().toUpperCase();
        String password = request.getPassword().toUpperCase();

        // Read user from security file (USRSEC VSAM -> users table)
        CardDemoUser user = userRepository.findById(userId)
                .orElseThrow(() -> CardDemoException.unauthorized("User not found. Try again ..."));

        // Password comparison - COBOL: IF SEC-USR-PWD = WS-USER-PWD
        if (!user.getUsrPwd().equals(password)) {
            throw CardDemoException.unauthorized("Wrong Password. Try again ...");
        }

        // Generate JWT token (replaces CICS COMMAREA-based session)
        String token = tokenProvider.generateToken(userId, user.getUsrType());

        return new LoginResponse(
                token,
                user.getUsrId(),
                user.getUsrType(),
                user.getUsrFname(),
                user.getUsrLname()
        );
    }
}
