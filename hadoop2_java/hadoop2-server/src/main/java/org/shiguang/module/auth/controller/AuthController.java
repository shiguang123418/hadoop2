package org.shiguang.module.auth.controller;

import org.shiguang.entity.User;
import org.shiguang.entity.dto.ApiResponse;
import org.shiguang.module.auth.service.AuthService;
import org.shiguang.module.auth.service.UserService;
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
    
    @Autowired
    private UserService userService;

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
    
    /**
     * 更新当前用户个人资料
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(@RequestBody Map<String, String> profileData) {
        User currentUser = authService.getCurrentUser();
        
        if (currentUser == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        
        // 只允许更新部分字段
        if (profileData.containsKey("nickname") || profileData.containsKey("name")) {
            currentUser.setName(profileData.getOrDefault("nickname", profileData.getOrDefault("name", currentUser.getName())));
        }
        
        if (profileData.containsKey("email")) {
            currentUser.setEmail(profileData.get("email"));
        }
        
        if (profileData.containsKey("phone")) {
            currentUser.setPhone(profileData.get("phone"));
        }
        
        User updatedUser = userService.updateUser(currentUser);
        
        return ResponseEntity.ok(ApiResponse.success("个人资料更新成功", updatedUser));
    }
    
    /**
     * 当前用户修改密码
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody Map<String, String> passwordData) {
        User currentUser = authService.getCurrentUser();
        
        if (currentUser == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        
        String currentPassword = passwordData.get("currentPassword");
        String newPassword = passwordData.get("newPassword");
        
        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.ok(ApiResponse.error(400, "当前密码和新密码不能为空"));
        }
        
        // 验证当前密码
        boolean passwordValid = authService.validatePassword(currentUser.getUsername(), currentPassword);
        
        if (!passwordValid) {
            return ResponseEntity.ok(ApiResponse.error(400, "当前密码不正确"));
        }
        
        // 修改密码
        boolean changed = userService.changePassword(currentUser.getId(), newPassword);
        
        if (!changed) {
            return ResponseEntity.ok(ApiResponse.error(500, "密码修改失败"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("密码修改成功"));
    }
} 