package com.example.Full_stack_Secure_Authentication_Ecosystem.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private String message;
    private UserInfo user;
    private String token;
    private String tokenType;
    private long tokenValidForSeconds;
    private long tokenExpireTime;

    public static class UserInfo {
        private Long id;
        private String name;
        private String email;

        public UserInfo(Long id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }

    public LoginResponse(
            String message,
            Long id,
            String name,
            String email,
            String token,
            long tokenValidForSeconds,
            long tokenExpireTime) {

        this.message = message;
        this.user = new UserInfo(id, name, email);
        this.token = token;
        this.tokenType = "Bearer";
        this.tokenValidForSeconds = tokenValidForSeconds;
        this.tokenExpireTime = tokenExpireTime;
    }

    public String getMessage() {
        return message;
    }

    public UserInfo getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getTokenValidForSeconds() {
        return tokenValidForSeconds;
    }

    public long getTokenExpireTime() {
        return tokenExpireTime;
    }
}
