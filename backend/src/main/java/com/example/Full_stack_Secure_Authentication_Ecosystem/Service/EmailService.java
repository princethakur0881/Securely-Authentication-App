
package com.example.Full_stack_Secure_Authentication_Ecosystem.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Service
public class EmailService {

    private final RestClient restClient;


    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${app.mail.from:onboarding@resend.dev}")
    private String fromEmail;

public EmailService() {
    this.restClient = RestClient.builder()
            .baseUrl("https://api.resend.com")
            .build();
}


    public void sendVerificationOtpEmail(String toEmail, String otp) {
        String html = """
                <div style="font-family:Arial,sans-serif">
                    <h2>Email Verification</h2>
                    <p>Your verification code is:</p>
                    <h1>%s</h1>
                    <p>If you did not request this code, ignore this email.</p>
                </div>
                """.formatted(escapeHtml(otp));

        sendEmail(toEmail, "Verify your Securely account", html);
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String html = """
                <div style="font-family:Arial,sans-serif">
                    <h2>Reset Your Password</h2>
                    <p>We received a request to reset your password.</p>
                    <p>
                        <a href="%s"
                           style="background:#2563eb;color:white;
                           padding:12px 20px;text-decoration:none;
                           border-radius:5px;display:inline-block;">
                           Reset Password
                        </a>
                    </p>
                    <p>If you did not request this, ignore this email.</p>
                </div>
                """.formatted(escapeHtml(resetLink));

        sendEmail(toEmail, "Reset Your Securely Password", html);
    }

    private void sendEmail(String toEmail, String subject, String html) {
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
