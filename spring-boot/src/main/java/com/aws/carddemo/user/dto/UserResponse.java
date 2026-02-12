package com.aws.carddemo.user.dto;

import java.time.LocalDateTime;

import com.aws.carddemo.user.AppUser;

public record UserResponse(
        Long id,
        String userId,
        String firstName,
        String lastName,
        String userType,
        Boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUserType(),
                user.getEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
