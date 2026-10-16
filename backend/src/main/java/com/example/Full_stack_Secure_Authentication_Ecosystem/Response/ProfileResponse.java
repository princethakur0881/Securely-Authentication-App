package com.example.Full_stack_Secure_Authentication_Ecosystem.Response;

import com.example.Full_stack_Secure_Authentication_Ecosystem.Entity.User;

public record ProfileResponse(Integer id, String fullName, String email, String role, boolean emailVerified) {
    public static ProfileResponse from(User user) {
        return new ProfileResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(), user.isEnabled());
    }
}
