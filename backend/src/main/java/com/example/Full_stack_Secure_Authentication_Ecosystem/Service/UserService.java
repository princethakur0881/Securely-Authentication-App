package com.example.Full_stack_Secure_Authentication_Ecosystem.Service;


import java.time.LocalDateTime;
import java.util.List;

import com.example.Full_stack_Secure_Authentication_Ecosystem.Dto.LoginDto;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Entity.Role;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Entity.User;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Exception.EmailAlreadyRegisteredException;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Repository.UserRepository;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Service.EmailVerificationService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;


@Service
public class UserService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationService emailVerificationService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            EmailVerificationService emailVerificationService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.emailVerificationService = emailVerificationService;
    }

    // Register a new user
    public User signup(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyRegisteredException(
                    "Email already registered"
            );
        }

        // Never trust the role sent by the client.
        user.setRole(Role.USER);

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);

        return savedUser;
    }


    // Authenticate an existing user
    public User authenticate(LoginDto loginDto) {

        String email = loginDto.getEmail().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid email or password"
                        )
                );

        unlockIfExpired(user);

        if (user.getLockTime() != null) {
            throw new LockedException(
                    "Account locked. Please try again after 15 minutes."
            );
        }



        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            loginDto.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            recordFailedLogin(user);

            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        recordSuccessfulLogin(user);
        return user;
    }
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));
    }



    // Get all users
    public List<User> getAllUser() {
        return userRepository.findAll();
    }

    private void unlockIfExpired(User user) {
        if (user.getLockTime() == null) {
            return;
        }

        LocalDateTime unlockAt = user.getLockTime()
                .plusMinutes(LOCK_DURATION_MINUTES);

        if (!LocalDateTime.now().isBefore(unlockAt)) {
            user.setLockTime(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }
    }

    private void recordFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockTime(LocalDateTime.now());
        }

        userRepository.save(user);
    }

    private void recordSuccessfulLogin(User user) {
        user.setFailedLoginAttempts(0);
        user.setLockTime(null);
        user.setLoginCount(user.getLoginCount() + 1);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
