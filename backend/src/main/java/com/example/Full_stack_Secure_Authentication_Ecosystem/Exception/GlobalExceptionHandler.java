
package com.example.Full_stack_Secure_Authentication_Ecosystem.Exception;

import io.jsonwebtoken.JwtException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(
            EmailAlreadyRegisteredException ex) {

        return response(HttpStatus.CONFLICT, "Email already registered");
    }

    @ExceptionHandler({AuthenticationException.class, JwtException.class})
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            Exception ex) {

        return response(
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials or authentication token"
        );
    }
@ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
public ResponseEntity<Map<String, Object>> handleDisabled(
        org.springframework.security.authentication.DisabledException ex) {

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            Map.of(
                    "status", 401,
                    "message", "Email is not verified. Verify your account first."
            )
    );
}

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex) {

        return response(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("Please check the submitted fields");

        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(
            HttpMessageNotReadableException ex) {

        return response(
                HttpStatus.BAD_REQUEST,
                "Request body is missing or contains invalid JSON"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {

        return response(
                HttpStatus.BAD_REQUEST,
                safeMessage(ex, "The submitted value is invalid")
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(
            Exception ex) {

        // Log the exception server-side; do not expose details to clients.
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The server could not complete the request"
        );
    }

    private ResponseEntity<Map<String, Object>> response(
            HttpStatus status, String message) {

        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "message", message
        ));
    }

    private String safeMessage(Exception ex, String fallback) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? fallback
                : ex.getMessage();
    }
}
