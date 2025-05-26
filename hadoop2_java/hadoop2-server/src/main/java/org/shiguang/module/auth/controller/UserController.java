package org.shiguang.module.auth.controller;

import org.shiguang.entity.User;
import org.shiguang.entity.dto.ApiResponse;
import org.shiguang.module.audit.AuditOperation;
import org.shiguang.module.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理相关的REST API
 */
@RestController
@RequestMapping("/auth/users")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private org.shiguang.utils.DataInitializer dataInitializer;

    /**
     * 获取所有用户
     */
    @GetMapping
    @AuditOperation(operation = "获取所有用户列表", operationType = "QUERY", resourceType = "USER")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("获取用户列表成功", users));
    }

    /**
     * 获取单个用户
     */
    @GetMapping("/{userId}")
    @AuditOperation(operation = "获取用户详情", operationType = "QUERY", resourceType = "USER", resourceIdIndex = 0)
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "用户不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success("获取用户信息成功", user));
    }

    /**
     * 创建用户
     */
    @PostMapping
    @AuditOperation(operation = "创建新用户", operationType = "CREATE", resourceType = "USER")
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.ok(ApiResponse.success("创建用户成功", createdUser));
    }

    /**
     * 更新用户
     */
    @PutMapping("/{userId}")
    @AuditOperation(operation = "更新用户信息", operationType = "UPDATE", resourceType = "USER", resourceIdIndex = 0)
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Long userId, @RequestBody User user) {
        user.setId(userId);
        User updatedUser = userService.updateUser(user);
        if (updatedUser == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "用户不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success("更新用户成功", updatedUser));
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{userId}")
    @AuditOperation(operation = "删除用户", operationType = "DELETE", resourceType = "USER", resourceIdIndex = 0)
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        boolean deleted = userService.deleteUser(userId);
        if (!deleted) {
            return ResponseEntity.ok(ApiResponse.error(404, "用户不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success("删除用户成功"));
    }

    /**
     * 更改用户密码
     */
    @PostMapping("/{userId}/change-password")
    @AuditOperation(operation = "管理员修改用户密码", operationType = "UPDATE", resourceType = "USER_PASSWORD", resourceIdIndex = 0)
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> passwordData) {
        
        String newPassword = passwordData.get("newPassword");
        boolean changed = userService.changePassword(userId, newPassword);
        
        if (!changed) {
            return ResponseEntity.ok(ApiResponse.error(404, "用户不存在"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("密码修改成功"));
    }

    /**
     * 更改用户状态
     */
    @PutMapping("/{userId}/status")
    @AuditOperation(operation = "修改用户状态", operationType = "UPDATE", resourceType = "USER_STATUS", resourceIdIndex = 0)
    public ResponseEntity<ApiResponse<User>> changeUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, String> statusData) {
        
        String status = statusData.get("status");
        User updatedUser = userService.changeUserStatus(userId, status);
        
        if (updatedUser == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "用户不存在"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("用户状态更新成功", updatedUser));
    }

    /**
     * 更改用户角色
     */
    @PutMapping("/{userId}/role")
    @AuditOperation(operation = "修改用户角色", operationType = "UPDATE", resourceType = "USER_ROLE", resourceIdIndex = 0)
    public ResponseEntity<ApiResponse<User>> changeUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> roleData) {
        
        String role = roleData.get("role");
        User updatedUser = userService.changeUserRole(userId, role);
        
        if (updatedUser == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "用户不存在"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("用户角色更新成功", updatedUser));
    }

    /**
     * 管理员重置用户密码为初始密码
     * 仅限ADMIN角色使用
     */
    @PostMapping("/{userId}/reset-password")
    @AuditOperation(operation = "重置用户密码", operationType = "UPDATE", resourceType = "USER_PASSWORD", resourceIdIndex = 0)
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long userId) {
        
        // 获取要重置的用户
        User user = userService.getUserById(userId);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "用户不存在"));
        }
        
        // 重置密码
        boolean reset = dataInitializer.resetUserPassword(user.getUsername());
        
        if (!reset) {
            return ResponseEntity.ok(ApiResponse.error(500, "密码重置失败"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("密码已重置为初始密码"));
    }
} 