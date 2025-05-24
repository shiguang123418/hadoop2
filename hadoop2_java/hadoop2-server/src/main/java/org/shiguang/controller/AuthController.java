package org.shiguang.controller;

import org.shiguang.security.service.AuthService;
import org.shiguang.entity.User;
import org.shiguang.entity.dto.LoginRequest;
import org.shiguang.entity.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        Map<String, Object> response = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());
        user.setEmail(registerRequest.getEmail());
        user.setFullName(registerRequest.getFullName());

        User createdUser = authService.register(user);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", createdUser.getId());
        response.put("username", createdUser.getUsername());
        response.put("message", "注册成功");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user")
    public ResponseEntity<Object> getCurrentUser() {
        User user = authService.getCurrentUser();
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        if (user == null) {
            errorResponse.put("message", "未认证");
            return ResponseEntity.status(401).body(errorResponse);
        }

        Map<String, Object> userResponse = new LinkedHashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put("username", user.getUsername());
        userResponse.put("email", user.getEmail());
        userResponse.put("fullName", user.getFullName());
        userResponse.put("roles", user.getRoles());
        return ResponseEntity.ok(userResponse);
    }
} 