package com.example.Full_stack_Secure_Authentication_Ecosystem.Entity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.*;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


@Entity
@Data
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private Integer id;

    private String fullName;
    @Column(nullable = false, unique = true)
    private String email;
    private String password;
    @Column(nullable = false)
    private boolean enabled =true ;
    @Column(nullable = false)
    private boolean emailVerified = false;
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private long loginCount = 0;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    private LocalDateTime lockTime;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    public User(Integer id, String fullName, String email, String password) {
        super();
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }

    public User() {

    }

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    //Getter and Setters

    //Override abstract method of UserDetails interface

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int attempts) {
        this.failedLoginAttempts = attempts;
    }

    public LocalDateTime getLockTime() {
        return lockTime;
    }

    public void setLockTime(LocalDateTime lockTime) {
        this.lockTime = lockTime;
    }

    @Override
    public boolean isAccountNonLocked() {
        return lockTime == null;
    }
    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }


}
