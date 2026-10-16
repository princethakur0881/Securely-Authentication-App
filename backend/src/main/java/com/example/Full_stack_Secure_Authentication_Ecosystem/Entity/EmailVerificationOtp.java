package com.example.Full_stack_Secure_Authentication_Ecosystem.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "email_verification_otps",
        indexes = {
                @Index(
                        name = "idx_email_verification_otp_user",
                        columnList = "user_id"
                )
        }
)
public class EmailVerificationOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public EmailVerificationOtp() {
    }

    public EmailVerificationOtp(
            User user,
            String otpHash,
            LocalDateTime expiryDate) {
        this.user = user;
        this.otpHash = otpHash;
        this.expiryDate = expiryDate;
        this.createdAt = LocalDateTime.now();
        this.failedAttempts = 0;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
