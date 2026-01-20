package com.carddemo.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String userId;
    private String userType;
    private String firstName;
    private String lastName;
    private String redirectUrl;
    private long expiresIn;
}
