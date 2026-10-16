package com.example.Full_stack_Secure_Authentication_Ecosystem.Response;

import com.example.Full_stack_Secure_Authentication_Ecosystem.Entity.User;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Integer id,
        String fullName,
        String email,
        boolean emailVerified,
        String role,
        long loginCount,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.isEnabled(), user.getRole().name(), user.getLoginCount(), user.getCreatedAt(), user.getLastLoginAt());
    }
}
