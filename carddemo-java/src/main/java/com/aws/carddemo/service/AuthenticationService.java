package com.aws.carddemo.service;

import com.aws.carddemo.dto.LoginRequest;
import com.aws.carddemo.dto.LoginResponse;
import com.aws.carddemo.entity.User;
import com.aws.carddemo.repository.UserRepository;
import com.aws.carddemo.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    public AuthenticationService(AuthenticationManager authenticationManager,
                                  JwtTokenProvider tokenProvider,
                                  UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public LoginResponse authenticate(LoginRequest request) {
        String userId = request.getUserId().toUpperCase();
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userId, request.getPassword())
        );

        String token = tokenProvider.generateToken(authentication);

        return LoginResponse.success(
                token,
                user.getUserId(),
                user.getUserType(),
                user.getUserFirstName(),
                user.getUserLastName()
        );
    }

    public boolean isAdminUser(String userId) {
        return userRepository.findByUserId(userId.toUpperCase())
                .map(User::isAdmin)
                .orElse(false);
    }
}
