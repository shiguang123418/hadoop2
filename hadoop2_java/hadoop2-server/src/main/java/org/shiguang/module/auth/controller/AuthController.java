package org.shiguang.module.auth.controller;

import org.shiguang.entity.User;
import org.shiguang.entity.dto.ApiResponse;
import org.shiguang.entity.dto.LoginRequest;
import org.shiguang.entity.dto.LoginResponseDTO;
import org.shiguang.entity.dto.UserDTO;
import org.shiguang.module.audit.AuditOperation;
import org.shiguang.module.auth.service.AuthService;
import org.shiguang.module.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 认证相关的REST API
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserService userService;


    /**
     * 用户登录
     */
    @PostMapping("/login")
    @AuditOperation(operation = "用户登录", operationType = "LOGIN", resourceType = "AUTH")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@RequestBody LoginRequest loginRequest) {
        try {
            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();
            
            // 输入验证
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(400, "用户名不能为空"));
            }
            
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(400, "密码不能为空"));
            }
            
            // 使用专门的登录服务处理登录
            LoginResponseDTO result = authService.login(username, password);
            
            return ResponseEntity.ok(ApiResponse.success("登录成功", result));
        } catch (BadCredentialsException e) {
            // 处理用户名或密码错误的情况
            logger.warn("登录失败: 用户名或密码错误 - {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(401, "用户名或密码错误"));
        } catch (UsernameNotFoundException e) {
            // 处理用户不存在的情况 - 为了安全，使用与密码错误相同的消息
            logger.warn("登录失败: 用户不存在 - {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(401, "用户名或密码错误"));
        } catch (IllegalStateException e) {
            // 处理账号状态问题
            logger.warn("登录失败: 账号状态异常 - {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(403, e.getMessage()));
        } catch (IllegalArgumentException e) {
            // 处理参数验证失败
            logger.warn("登录失败: 参数验证失败 - {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            // 处理其他未预期的异常
            logger.error("登录过程中发生未知异常:", e);
            return ResponseEntity.ok(ApiResponse.error(500, "登录失败，请稍后再试"));
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @AuditOperation(operation = "用户注册", operationType = "REGISTER", resourceType = "USER")
    public ResponseEntity<ApiResponse<UserDTO>> register(@RequestBody User user) {
        User createdUser = authService.register(user);
        // 使用DTO过滤敏感信息
        UserDTO userDTO = UserDTO.fromUser(createdUser);
        
        return ResponseEntity.ok(ApiResponse.success("注册成功", userDTO));
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    @AuditOperation(operation = "获取当前用户信息", operationType = "QUERY", resourceType = "USER")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        User currentUser = authService.getCurrentUser();
        
        if (currentUser == null) {
            logger.warn("getCurrentUser: 未找到当前登录用户");
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        
        // 添加详细日志，确认头像URL是否存在
        logger.info("getCurrentUser: 用户 {} 获取成功, ID: {}, 头像: {}", 
                 currentUser.getUsername(),
                 currentUser.getId(),
                 currentUser.getAvatar() != null ? currentUser.getAvatar() : "无");
        
        // 确保头像URL不为空
        if (currentUser.getAvatar() == null || currentUser.getAvatar().trim().isEmpty()) {
            logger.info("getCurrentUser: 用户没有头像，设置为null");
        } else {
            logger.info("getCurrentUser: 返回用户头像URL: {}", currentUser.getAvatar());
        }
        
        // 使用UserDTO过滤敏感字段
        UserDTO userDTO = UserDTO.fromUser(currentUser);
        
        return ResponseEntity.ok(ApiResponse.success("获取当前用户信息成功", userDTO));
    }
    
    /**
     * 更新当前用户个人资料
     */
    @PutMapping("/profile")
    @AuditOperation(operation = "更新个人资料", operationType = "UPDATE", resourceType = "USER_PROFILE")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile(@RequestBody Map<String, String> profileData) {
        User currentUser = authService.getCurrentUser();
        
        if (currentUser == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        
        // 单独处理头像更新，与其他资料分开
        String avatarUrl = null;
        if (profileData.containsKey("avatar")) {
            avatarUrl = profileData.get("avatar");
            profileData.remove("avatar"); // 从profileData移除，防止被常规更新处理
            
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                logger.info("检测到头像更新请求，使用安全方法单独处理头像: {}", avatarUrl);
                try {
                    User updatedWithAvatar = userService.updateUserAvatar(currentUser.getId(), avatarUrl);
                    if (updatedWithAvatar == null) {
                        logger.error("头像更新失败: 用户不存在或更新过程出错");
                        return ResponseEntity.ok(ApiResponse.error(404, "更新头像失败"));
                    }
                    currentUser = updatedWithAvatar; // 使用更新后的用户对象继续处理
                    logger.info("头像已安全更新: {}", avatarUrl);
                } catch (Exception e) {
                    logger.error("更新头像失败: {}", e.getMessage());
                    return ResponseEntity.ok(ApiResponse.error(500, "头像更新失败: " + e.getMessage()));
                }
            }
        }
        
        // 如果没有其他字段要更新，直接返回
        if (profileData.isEmpty() || (profileData.size() == 1 && profileData.containsKey("avatar"))) {
            logger.info("没有需要更新的基本资料字段，返回当前用户信息");
            return ResponseEntity.ok(ApiResponse.success("用户资料更新成功", UserDTO.fromUser(currentUser)));
        }
        
        // 处理其他基本信息
        logger.info("更新用户基本资料: {}", profileData);
        
        // 获取数据库中的完整用户信息，确保不丢失密码等敏感字段
        User fullUser = userService.getUserById(currentUser.getId());
        if (fullUser == null) {
            logger.error("更新用户资料失败: 数据库中未找到用户 ID={}", currentUser.getId());
            return ResponseEntity.ok(ApiResponse.error(404, "用户不存在"));
        }
        
        // 记录更新前的状态
        logger.info("更新用户资料前: ID={}, 用户名={}, 密码状态={}", 
                  fullUser.getId(), 
                  fullUser.getUsername(),
                  fullUser.getPassword() != null ? "已设置" : "未设置");
        
        // 只允许更新部分字段
        if (profileData.containsKey("nickname") || profileData.containsKey("name")) {
            fullUser.setName(profileData.getOrDefault("nickname", profileData.getOrDefault("name", fullUser.getName())));
        }
        
        if (profileData.containsKey("email")) {
            fullUser.setEmail(profileData.get("email"));
        }
        
        if (profileData.containsKey("phone")) {
            fullUser.setPhone(profileData.get("phone"));
        }
        
        // 确认密码字段在更新前是否完整
        logger.info("更新用户前最终检查: ID={}, 用户名={}, 密码状态={}", 
                  fullUser.getId(), 
                  fullUser.getUsername(),
                  fullUser.getPassword() != null ? "已设置" : "未设置");
        
        User updatedUser = userService.updateUser(fullUser);
        
        // 如果之前更新了头像，确保在响应中包含头像URL
        if (avatarUrl != null && !avatarUrl.trim().isEmpty() && updatedUser != null) {
            updatedUser.setAvatar(avatarUrl);
        }
        
        if (updatedUser != null) {
            logger.info("用户资料更新成功，完整资料已更新");
        }
        
        // 使用UserDTO过滤敏感字段
        return ResponseEntity.ok(ApiResponse.success("个人资料更新成功", UserDTO.fromUser(updatedUser)));
    }
    
    /**
     * 当前用户修改密码
     */
    @PostMapping("/change-password")
    @AuditOperation(operation = "修改个人密码", operationType = "UPDATE", resourceType = "USER_PASSWORD")
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
    
    /**
     * 用户注销登录
     */
    @PostMapping("/logout")
    @AuditOperation(operation = "用户注销", operationType = "LOGOUT", resourceType = "AUTH")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // 这里根据实际情况实现注销逻辑
        return ResponseEntity.ok(ApiResponse.success("注销成功"));
    }
} 