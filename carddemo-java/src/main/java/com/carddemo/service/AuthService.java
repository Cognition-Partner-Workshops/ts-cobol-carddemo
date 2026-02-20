package com.carddemo.service;

import com.carddemo.dto.LoginRequest;
import com.carddemo.dto.LoginResponse;
import com.carddemo.entity.UserSecurity;
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserSecurityRepository userSecurityRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserSecurityRepository userSecurityRepository,
                       JwtTokenProvider jwtTokenProvider) {
        this.userSecurityRepository = userSecurityRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse authenticate(LoginRequest request) {
        String userId = request.getUserId().toUpperCase().trim();
        String password = request.getPassword().toUpperCase().trim();

        UserSecurity user = userSecurityRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found. Try again ..."));

        if (!user.getUsrPwd().trim().equals(password)) {
            throw new IllegalArgumentException("Wrong Password. Try again ...");
        }

        String token = jwtTokenProvider.generateToken(userId, user.getUsrType());

        return new LoginResponse(
                token,
                userId,
                user.getUsrFname().trim(),
                user.getUsrLname().trim(),
                user.getUsrType()
        );
    }
}
