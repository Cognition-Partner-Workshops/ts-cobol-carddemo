package com.aws.carddemo.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    
    @NotBlank(message = "User ID is required")
    @Size(max = 8, message = "User ID must be at most 8 characters")
    private String userId;
    
    @NotBlank(message = "Password is required")
    @Size(max = 8, message = "Password must be at most 8 characters")
    private String password;
}
