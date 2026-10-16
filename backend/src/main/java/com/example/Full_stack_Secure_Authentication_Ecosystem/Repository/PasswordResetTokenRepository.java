package com.example.Full_stack_Secure_Authentication_Ecosystem.Repository;

import com.example.Full_stack_Secure_Authentication_Ecosystem.Entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteByUser_Id(Long userId);
}