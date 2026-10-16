package com.example.Full_stack_Secure_Authentication_Ecosystem.Repository;

import com.example.Full_stack_Secure_Authentication_Ecosystem.Entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);
}
