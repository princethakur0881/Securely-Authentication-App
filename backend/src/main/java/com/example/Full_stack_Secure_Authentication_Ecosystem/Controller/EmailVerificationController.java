package com.example.Full_stack_Secure_Authentication_Ecosystem.Controller;

import com.example.Full_stack_Secure_Authentication_Ecosystem.Service.EmailVerificationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(
            EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    // Send a 6-digit OTP to the logged-in user's email
    @PostMapping("/auth/verification-otp/send")
    public ResponseEntity<?> sendVerificationOtp(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof UserDetails userDetails)) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Authentication required"
                    ));
        }

        try {
            String email = userDetails.getUsername();

            emailVerificationService.sendOtp(email);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "OTP sent successfully. Please check your email."
            ));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "message", "Unable to send OTP. Please try again."
                    ));
        }
    }

    // Verify the OTP entered by the logged-in user
    @PostMapping("/auth/verification-otp/verify")
    public ResponseEntity<?> verifyVerificationOtp(
            Authentication authentication,
            @RequestBody VerifyOtpRequest request) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof UserDetails userDetails)) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Authentication required"
                    ));
        }

        try {
            String email = userDetails.getUsername();

            emailVerificationService.verifyOtp(
                    email,
                    request.getOtp()
            );

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "verified", true,
                    "message", "Email verified successfully."
            ));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "message", "Unable to verify OTP. Please try again."
                    ));
        }
    }

    // Request body for OTP verification
    public static class VerifyOtpRequest {

        private String otp;

        public VerifyOtpRequest() {
        }

        public String getOtp() {
            return otp;
        }

        public void setOtp(String otp) {
            this.otp = otp;
        }
    }
}
