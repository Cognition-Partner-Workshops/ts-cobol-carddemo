package com.carddemo.auth.service;

import com.carddemo.auth.dto.LoginRequest;
import com.carddemo.auth.dto.LoginResponse;
import com.carddemo.auth.dto.UserDto;
import com.carddemo.auth.entity.User;
import com.carddemo.auth.repository.UserRepository;
import com.carddemo.auth.security.JwtTokenProvider;
import com.carddemo.common.exception.BusinessException;
import com.carddemo.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUserIdAndActive(request.getUserId(), true)
                .orElseThrow(() -> new BusinessException("Invalid userid and/or password", "AUTH_FAILED"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid userid and/or password", "AUTH_FAILED");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = tokenProvider.generateToken(user.getUserId(), user.getUserType().name());

        String redirectUrl = user.getUserType() == User.UserType.A ? "/admin" : "/main";

        return LoginResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .userType(user.getUserType().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .redirectUrl(redirectUrl)
                .expiresIn(tokenProvider.getExpirationTime())
                .build();
    }

    public UserDto getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        return UserDto.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userType(user.getUserType().name())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }

    public void logout(String userId) {
        // In a stateless JWT system, logout is handled client-side
        // For additional security, you could implement token blacklisting here
    }
}
