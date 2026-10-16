package com.example.Full_stack_Secure_Authentication_Ecosystem.Response;

public record AdminDashboardResponse(
        String message,
        long registeredUsers,
        long verifiedUsers,
        long pendingVerification,
        long totalSuccessfulLogins,
        long usersWithLogin
) {}
