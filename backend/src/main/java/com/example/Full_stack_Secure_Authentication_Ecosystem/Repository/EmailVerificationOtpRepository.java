
package com.example.Full_stack_Secure_Authentication_Ecosystem.Repository;

import com.example.Full_stack_Secure_Authentication_Ecosystem.Entity.EmailVerificationOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationOtpRepository
        extends JpaRepository<EmailVerificationOtp, Long> {

    Optional<EmailVerificationOtp> findByUser_Id(Integer userId);

    void deleteByUser_Id(Integer userId);
}
