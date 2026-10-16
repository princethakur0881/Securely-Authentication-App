package com.example.Full_stack_Secure_Authentication_Ecosystem.Controller;

import com.example.Full_stack_Secure_Authentication_Ecosystem.Service.EmailVerificationService;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Service.JWTService;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Service.UserService;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Entity.User;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Dto.LoginDto;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Dto.RegisterRequest;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Response.LoginResponse;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Response.ProfileResponse;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import java.util.List;

@RestController
public class UserController {

    private final JWTService jwtService;
    private final UserService userService;


    public UserController(
            JWTService jwtService,
            UserService userService, EmailVerificationService emailVerificationService) {
        this.jwtService = jwtService;
        this.userService = userService;

    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        User user = new User(null, request.fullName().trim(), request.email().trim().toLowerCase(), request.password());

        User registeredUser = userService.signup(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "status", 201,
                        "message", "Registration successful. Please verify your email.",
                        "user", Map.of(
                                "id", registeredUser.getId(),
                                "fullName", registeredUser.getFullName(),
                                "email", registeredUser.getEmail(),
                                "emailVerified", registeredUser.isEnabled()
                        )
                )
        );
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> authenticate(
            @Valid @RequestBody LoginDto loginDto) {

        User authenticatedUser =
                userService.authenticate(loginDto);

        String token = jwtService.generateToken(
                new java.util.HashMap<>(),
                authenticatedUser
        );

        long validitySeconds =
                jwtService.getExpirationTime() / 1000;

        LoginResponse response = new LoginResponse(
                "Welcome back, " +
                        authenticatedUser.getFullName() +
                        "! Login successful.",
                authenticatedUser.getId().longValue(),
                authenticatedUser.getFullName(),
                authenticatedUser.getEmail(),
                token,
                validitySeconds,
                jwtService.getExpirationTime()
        );

        return ResponseEntity.ok(response);
    }


    @GetMapping("/auth/profile")
    public ResponseEntity<?> profile(Authentication authentication) {

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required"));
        }

        User user = userService.findByEmail(userDetails.getUsername());

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "emailVerified", user.isEmailVerified()
        ));
    }

}
