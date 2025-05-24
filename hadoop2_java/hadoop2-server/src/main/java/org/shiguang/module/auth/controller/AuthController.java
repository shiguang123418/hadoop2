package org.shiguang.module.auth.controller;

import org.shiguang.entity.User;
import org.shiguang.entity.dto.ApiResponse;
import org.shiguang.module.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证相关的REST API
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        
        Map<String, Object> result = authService.login(username, password);
        
        return ResponseEntity.ok(ApiResponse.success("登录成功", result));
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@RequestBody User user) {
        User createdUser = authService.register(user);
        
        return ResponseEntity.ok(ApiResponse.success("注册成功", createdUser));
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<User>> getCurrentUser() {
        User currentUser = authService.getCurrentUser();
        
        if (currentUser == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("获取当前用户信息成功", currentUser));
    }
} 