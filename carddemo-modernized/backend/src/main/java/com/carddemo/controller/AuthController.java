package com.carddemo.controller;

import com.carddemo.dto.*;
import com.carddemo.model.User;
import com.carddemo.security.JwtTokenProvider;
import com.carddemo.security.UserPrincipal;
import com.carddemo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUserId().toUpperCase(),
                        request.getPassword()
                )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        LoginResponse response = LoginResponse.builder()
                .token(jwt)
                .userId(userPrincipal.getUserId())
                .firstName(userPrincipal.getFirstName())
                .lastName(userPrincipal.getLastName())
                .userType(userPrincipal.getUserType())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", user));
    }
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        LoginResponse response = LoginResponse.builder()
                .userId(userPrincipal.getUserId())
                .firstName(userPrincipal.getFirstName())
                .lastName(userPrincipal.getLastName())
                .userType(userPrincipal.getUserType())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
