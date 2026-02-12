package com.aws.carddemo.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 255) String password,
        @Size(max = 25) String firstName,
        @Size(max = 25) String lastName,
        @Pattern(regexp = "[AU]") String userType
) {}
