
package com.example.Full_stack_Secure_Authentication_Ecosystem.Service;

import com.example.Full_stack_Secure_Authentication_Ecosystem.Entity.PasswordResetToken;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Entity.User;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Repository.PasswordResetTokenRepository;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.password-reset.base-url}")
    private String resetBaseUrl;


    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }
@Transactional
    public void forgotPassword(String email) {

        Optional<User> optionalUser =
                userRepository.findByEmail(email);

        // Do not reveal whether the email is registered.
        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();

        tokenRepository.deleteByUser_Id(user.getId().longValue());

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken =
                new PasswordResetToken(
                        tokenHash,
                        user,
                        LocalDateTime.now().plusMinutes(15)
                );

        tokenRepository.save(resetToken);

//    String resetUrl = resetBaseUrl
//            + "?token="
//            + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    String resetUrl = resetBaseUrl + "?token=" + rawToken;
    emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);

//    emailService.sendPasswordResetEmail(
//            user.getEmail(),
//            resetUrl
//    );

    }

    @Transactional
    public void resetPassword(
            String rawToken,
            String newPassword) {

        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken =
                tokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid or expired reset link"
                                ));

        if (!resetToken.getExpiryDate()
                .isAfter(LocalDateTime.now())) {

            tokenRepository.delete(resetToken);

            throw new IllegalArgumentException(
                    "Invalid or expired reset link"
            );
        }

        User user = resetToken.getUser();

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // A reset token can be used only once.
        tokenRepository.delete(resetToken);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);

        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "Unable to hash reset token", e
            );
        }
    }
}
