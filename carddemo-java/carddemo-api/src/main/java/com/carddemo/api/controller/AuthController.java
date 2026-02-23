package com.carddemo.api.controller;

import com.carddemo.api.dto.LoginRequest;
import com.carddemo.api.dto.LoginResponse;
import com.carddemo.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication REST controller.
 * Replaces CICS transaction CC00 (COSGN00C - Signon Screen).
 *
 * COBOL → Java mapping:
 *   CC00 SEND MAP('COSGN0A') → POST /api/auth/login (JSON request)
 *   CC00 RECEIVE MAP('COSGN0A') → POST /api/auth/login (JSON response with JWT)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication (replaces CICS CC00)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates credentials and returns JWT token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidates the current session token")
    public ResponseEntity<Void> logout() {
        // TODO: Implement JWT token invalidation in Phase 3
        return ResponseEntity.ok().build();
    }
}
