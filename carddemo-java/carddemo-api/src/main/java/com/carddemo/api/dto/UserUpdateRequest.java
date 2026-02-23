package com.carddemo.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User update request DTO replacing BMS map COUSR02 input fields.
 * Used for COUSR02C (Update User) → PUT /api/admin/users/{userId}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @Size(max = 20, message = "First name must not exceed 20 characters")
    private String firstName;

    @Size(max = 20, message = "Last name must not exceed 20 characters")
    private String lastName;

    @Size(min = 4, max = 8, message = "Password must be 4-8 characters")
    private String password;

    @Pattern(regexp = "[AU]", message = "User type must be 'A' (Admin) or 'U' (User)")
    private String userType;
}
