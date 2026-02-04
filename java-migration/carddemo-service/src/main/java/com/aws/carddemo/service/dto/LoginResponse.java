package com.aws.carddemo.service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String userId;
    private String firstName;
    private String lastName;
    private String userType;
    private String token;
    private long expiresIn;
}
