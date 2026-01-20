package com.carddemo.admin.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    @Size(max = 20, message = "First name must be at most 20 characters")
    private String firstName;

    @Size(max = 20, message = "Last name must be at most 20 characters")
    private String lastName;

    @Size(min = 1, max = 8, message = "Password must be between 1 and 8 characters")
    private String password;

    @Pattern(regexp = "^[AU]$", message = "User type must be A (Admin) or U (User)")
    private String userType;

    private Boolean active;
}
