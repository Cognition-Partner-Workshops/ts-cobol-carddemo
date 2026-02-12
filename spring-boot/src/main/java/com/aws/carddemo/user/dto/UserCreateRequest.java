package com.aws.carddemo.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(max = 8) String userId,
        @NotBlank @Size(max = 255) String password,
        @NotBlank @Size(max = 25) String firstName,
        @NotBlank @Size(max = 25) String lastName,
        @NotBlank @Pattern(regexp = "[AU]") String userType
) {}
