package com.example.Full_stack_Secure_Authentication_Ecosystem.Controller;

import com.example.Full_stack_Secure_Authentication_Ecosystem.Entity.User;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Response.AdminDashboardResponse;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Response.AdminUserResponse;
import com.example.Full_stack_Secure_Authentication_Ecosystem.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        List<User> users = userService.getAllUser();
        long verified = users.stream().filter(User::isEnabled).count();
        long logins = users.stream().mapToLong(User::getLoginCount).sum();
        long usersWithLogin = users.stream().filter(user -> user.getLoginCount() > 0).count();
        return new AdminDashboardResponse("Admin dashboard", users.size(), verified, users.size() - verified, logins, usersWithLogin);
    }

    @GetMapping("/getUsers")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        List<AdminUserResponse> users = userService.getAllUser().stream()
                .map(AdminUserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }
}
