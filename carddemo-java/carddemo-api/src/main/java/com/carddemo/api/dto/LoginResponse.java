package com.carddemo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Login response DTO with JWT token.
 * Replaces CICS COMMAREA user session data from COSGN00C.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private String userId;
    private String userType;
    private String firstName;
    private String lastName;
}
