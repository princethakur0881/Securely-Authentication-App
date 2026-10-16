
package com.example.Full_stack_Secure_Authentication_Ecosystem.Service;

import com.example.Full_stack_Secure_Authentication_Ecosystem.Entity.EmailVerificationOtp;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Entity.User;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Repository.EmailVerificationOtpRepository;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Repository.UserRepository;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class EmailVerificationService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_VALID_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final EmailVerificationOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(
            EmailVerificationOtpRepository otpRepository,
            UserRepository userRepository,
            EmailService emailService) {

        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void sendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Unable to process verification request"
                        )
                );

        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email is already verified");
        }

        // Invalidate any previous OTP.
        otpRepository.deleteByUser_Id(user.getId());

        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        EmailVerificationOtp otpEntity =
                new EmailVerificationOtp(
                        user,
                        hashOtp(otp),
                        LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES)
                );

        otpRepository.save(otpEntity);

        emailService.sendVerificationOtpEmail(user.getEmail(), otp);
    }

    @Transactional
    public void verifyOtp(String email, String submittedOtp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid or expired OTP"
                        )
                );

        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email is already verified");
        }

        if (submittedOtp == null ||
                !submittedOtp.matches("\\d{6}")) {
            throw new IllegalArgumentException("Enter a valid 6-digit OTP");
        }

        EmailVerificationOtp otpEntity =
                otpRepository.findByUser_Id(user.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid or expired OTP"
                                )
                        );

        if (!otpEntity.getExpiryDate().isAfter(LocalDateTime.now())) {
            otpRepository.delete(otpEntity);
            throw new IllegalArgumentException("OTP has expired. Request a new one.");
        }

        if (otpEntity.getFailedAttempts() >= MAX_ATTEMPTS) {
            otpRepository.delete(otpEntity);
            throw new IllegalArgumentException(
                    "Too many incorrect attempts. Request a new OTP."
            );
        }

        String submittedHash = hashOtp(submittedOtp);

        if (!MessageDigest.isEqual(
                submittedHash.getBytes(StandardCharsets.UTF_8),
                otpEntity.getOtpHash().getBytes(StandardCharsets.UTF_8))) {

            otpEntity.setFailedAttempts(
                    otpEntity.getFailedAttempts() + 1
            );

            if (otpEntity.getFailedAttempts() >= MAX_ATTEMPTS) {
                otpRepository.delete(otpEntity);
            } else {
                otpRepository.save(otpEntity);
            }

            throw new IllegalArgumentException("Incorrect OTP");
        }

        user.setEmailVerified(true);
        userRepository.save(user);
        otpRepository.delete(otpEntity);
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    otp.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "Unable to hash OTP", e
            );
        }
    }
}
