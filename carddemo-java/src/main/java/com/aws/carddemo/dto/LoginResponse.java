package com.aws.carddemo.dto;

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
    private String message;

    public static LoginResponse success(String token, String userId, String userType, String firstName, String lastName) {
        return LoginResponse.builder()
                .token(token)
                .userId(userId)
                .userType(userType)
                .firstName(firstName)
                .lastName(lastName)
                .message("Login successful")
                .build();
    }

    public static LoginResponse error(String message) {
        return LoginResponse.builder()
                .message(message)
                .build();
    }
}
