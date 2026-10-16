package com.example.Full_stack_Secure_Authentication_Ecosystem.Security;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<String, Window> clients =
            new ConcurrentHashMap<>();

    private static class Window {
        long startTime;
        int requests;

        Window(long startTime) {
            this.startTime = startTime;
            this.requests = 1;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && "/auth/login".equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        long now = System.currentTimeMillis();

        Window window = clients.compute(ip, (key, old) -> {
            if (old == null || now - old.startTime >= WINDOW_MS) {
                return new Window(now);
            }

            old.requests++;
            return old;
        });

        if (window.requests > MAX_REQUESTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"message\":\"Too many login requests. Try later.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}