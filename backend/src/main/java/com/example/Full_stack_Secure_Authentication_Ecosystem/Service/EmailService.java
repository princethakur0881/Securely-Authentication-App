package com.example.Full_stack_Secure_Authentication_Ecosystem.Service;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.stereotype.Service;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.mail.MailException;
//@Service
//public class EmailService {
//    private static final Logger log =
//            LoggerFactory.getLogger(EmailService.class);
//    private final JavaMailSender javaMailSender;
//
//    @Value("${app.mail.from}")
//    private String fromEmail;
//
//    public EmailService(JavaMailSender javaMailSender) {
//        this.javaMailSender = javaMailSender;
//    }
//
//
//    //Send mail to reset passworsd link
//    public void sendPasswordResetEmail(
//            String to,
//            String resetUrl) {
//
//        SimpleMailMessage message = new SimpleMailMessage();
//
//        message.setFrom(fromEmail);
//        message.setTo(to);
//        message.setSubject("Reset Your Password");
//
//        message.setText(
//                "We received a request to reset your password.\n\n"
//                        + "Use the link below to reset it:\n"
//                        + resetUrl
//                        + "\n\nThis link expires in 15 minutes.\n"
//                        + "If you did not request this, ignore this email."
//        );
//
//        javaMailSender.send(message);
//    }
//        public void sendVerificationOtpEmail(String to, String otp) {
//            SimpleMailMessage message = new SimpleMailMessage();
//
//            message.setFrom(fromEmail);
//            message.setTo(to);
//            message.setSubject("Your Email Verification OTP");
//
//            message.setText(
//                    "Your email verification OTP is: " + otp
//                            + "\n\nThis OTP expires in 10 minutes."
//                            + "\nDo not share this OTP with anyone."
//                            + "\nIf you did not request this, ignore this email."
//            );
//            try {
//                log.info("Attempting to send verification email");
//                javaMailSender.send(message);
//                log.info("Verification email sent successfully");
//            } catch (MailException e) {
//                log.error("Failed to send verification email", e);
//                throw e;
//            }
//        }
//}
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Service
public class EmailService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${app.mail.from:onboarding@resend.dev}")
    private String fromEmail;

    public EmailService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.resend.com")
                .build();
        this.objectMapper = objectMapper;
    }

    public void sendVerificationOtpEmail(String toEmail, String otp) {
        String subject = "Verify your email - Securely";

        String html = """
                <div style="font-family:Arial,sans-serif">
                    <h2>Email Verification</h2>
                    <p>Your Securely verification code is:</p>
                    <h1 style="letter-spacing:5px">%s</h1>
                    <p>Enter this code in the app to verify your email.</p>
                    <p>If you did not request this code, ignore this email.</p>
                </div>
                """.formatted(escapeHtml(otp));

        Map<String, Object> body = Map.of(
                "from", fromEmail,
                "to", new String[]{toEmail},
                "subject", subject,
                "html", html
        );

        try {
            restClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + resendApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Resend API failed with HTTP "
                            + e.getStatusCode().value()
                            + ": " + e.getResponseBodyAsString(),
                    e
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to send verification email through Resend.", e
            );
        }
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}