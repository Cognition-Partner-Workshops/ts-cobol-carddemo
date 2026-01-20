package com.carddemo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "User ID is required")
    @Size(min = 1, max = 8, message = "User ID must be between 1 and 8 characters")
    private String userId;

    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 8, message = "Password must be between 1 and 8 characters")
    private String password;
}
