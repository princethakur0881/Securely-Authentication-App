package com.example.Full_stack_Secure_Authentication_Ecosystem.Controller;

import com.example.Full_stack_Secure_Authentication_Ecosystem.Dto.ForgotPasswordRequest;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Dto.ResetPasswordRequest;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Service.PasswordResetService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(
            PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        passwordResetService.forgotPassword(request.email());

        return ResponseEntity.ok(Map.of(
                "message",
                "If the email is registered, a reset link will be sent."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        try {
            passwordResetService.resetPassword(
                    request.token(),
                    request.newPassword()
            );

            return ResponseEntity.ok(Map.of(
                    "message",
                    "Password reset successful. Please log in."
            ));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", ex.getMessage()
            ));
        }
    }
}