package com.aws.carddemo.service.auth;

import com.aws.carddemo.domain.entity.User;
import com.aws.carddemo.domain.repository.UserRepository;
import com.aws.carddemo.service.dto.LoginRequest;
import com.aws.carddemo.service.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Authentication Service - migrated from COSGN00C.cbl
 * Handles user login and authentication
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Authenticate user and generate JWT token
     * Migrated from READ-USER-SEC-FILE paragraph in COSGN00C.cbl
     */
    @Transactional
    public LoginResponse authenticate(LoginRequest request) {
        log.info("Attempting authentication for user: {}", request.getUserId());

        String userId = request.getUserId().toUpperCase();

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", userId);
                    return new UsernameNotFoundException("User not found. Try again ...");
                });

        if (!user.getEnabled()) {
            log.warn("User account is disabled: {}", userId);
            throw new BadCredentialsException("User account is disabled");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userId, request.getPassword().toUpperCase())
            );

            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            String token = jwtService.generateToken(user);
            long expiresIn = jwtService.getExpirationTime();

            log.info("Authentication successful for user: {}", userId);

            return LoginResponse.builder()
                    .userId(user.getUserId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .userType(user.getUserType())
                    .token(token)
                    .expiresIn(expiresIn)
                    .build();

        } catch (Exception e) {
            log.warn("Authentication failed for user: {} - {}", userId, e.getMessage());
            throw new BadCredentialsException("Wrong Password. Try again ...");
        }
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        return jwtService.isTokenValid(token);
    }

    /**
     * Extract user ID from token
     */
    public String extractUserId(String token) {
        return jwtService.extractUsername(token);
    }
}
