package com.example.Full_stack_Secure_Authentication_Ecosystem.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
@Service
public class EmailService {
    private static final Logger log =
            LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }


    //Send mail to reset passworsd link
    public void sendPasswordResetEmail(
            String to,
            String resetUrl) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Reset Your Password");

        message.setText(
                "We received a request to reset your password.\n\n"
                        + "Use the link below to reset it:\n"
                        + resetUrl
                        + "\n\nThis link expires in 15 minutes.\n"
                        + "If you did not request this, ignore this email."
        );

        javaMailSender.send(message);
    }
        public void sendVerificationOtpEmail(String to, String otp) {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Your Email Verification OTP");

            message.setText(
                    "Your email verification OTP is: " + otp
                            + "\n\nThis OTP expires in 10 minutes."
                            + "\nDo not share this OTP with anyone."
                            + "\nIf you did not request this, ignore this email."
            );
            try {
                log.info("Attempting to send verification email");
                javaMailSender.send(message);
                log.info("Verification email sent successfully");
            } catch (MailException e) {
                log.error("Failed to send verification email", e);
                throw e;
            }
        }
}