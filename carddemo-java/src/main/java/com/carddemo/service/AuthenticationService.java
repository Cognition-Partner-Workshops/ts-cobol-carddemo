package com.carddemo.service;

import com.carddemo.dto.LoginRequest;
import com.carddemo.dto.LoginResponse;
import com.carddemo.entity.User;
import com.carddemo.repository.UserRepository;
import com.carddemo.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

/**
 * Authentication service - migrated from COSGN00C (CC00 Sign-on Screen).
 *
 * Original COBOL logic:
 * 1. Receive user ID and password from BMS map COSGN0A
 * 2. Read USRSEC file by user ID key
 * 3. Compare password
 * 4. On match: set COMMAREA fields and XCTL to COADM01C (admin) or COMEN01C (user)
 * 5. On mismatch: display error message
 */
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthenticationService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Authenticate a user and return a JWT token.
     * Migrated from COSGN00C READ-USER-SEC-FILE paragraph.
     */
    public LoginResponse authenticate(LoginRequest request) {
        String userId = request.getUserId().toUpperCase().trim();
        String password = request.getPassword().toUpperCase().trim();

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found. Try again ..."));

        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("Wrong Password. Try again ...");
        }

        String token = jwtTokenProvider.generateToken(userId, user.getUserType());

        return new LoginResponse(
                token,
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUserType()
        );
    }
}
