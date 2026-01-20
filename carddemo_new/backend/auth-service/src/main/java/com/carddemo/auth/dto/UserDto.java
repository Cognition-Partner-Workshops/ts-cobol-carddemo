package com.carddemo.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String userId;
    private String firstName;
    private String lastName;
    private String userType;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}
