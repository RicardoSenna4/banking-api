package com.ricardosenna.bankingapi.dto;

import com.ricardosenna.bankingapi.entity.UserEntity;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        String role,
        boolean active,
        Instant createdAt
) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
