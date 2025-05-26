package org.shiguang.module.auth.controller;

import org.shiguang.entity.User;
import org.shiguang.entity.dto.ApiResponse;
import org.shiguang.module.audit.AuditOperation;
import org.shiguang.module.auth.service.AuthService;
import org.shiguang.module.auth.service.UserService;
import org.shiguang.module.oss.service.OssService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户个人资料API
 */
@RestController
@RequestMapping("/user/profile")
@CrossOrigin
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private OssService ossService;

    /**
     * 获取当前用户个人资料
     */
    @GetMapping
    @AuditOperation(operation = "查看个人资料", operationType = "QUERY", resourceType = "USER_PROFILE")
    public ResponseEntity<ApiResponse<User>> getUserProfile() {
        User currentUser = authService.getCurrentUser();
        
        if (currentUser == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("获取个人资料成功", currentUser));
    }
    
    /**
     * 更新个人资料
     */
    @PutMapping
    @AuditOperation(operation = "更新个人资料", operationType = "UPDATE", resourceType = "USER_PROFILE")
    public ResponseEntity<ApiResponse<User>> updateProfile(@RequestBody Map<String, String> profileData) {
        User currentUser = authService.getCurrentUser();
        
        if (currentUser == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        
        logger.info("收到个人资料更新请求: {}", profileData);
        
        // 更新基本信息
        if (profileData.containsKey("name")) {
            currentUser.setName(profileData.get("name"));
        }
        
        if (profileData.containsKey("email")) {
            currentUser.setEmail(profileData.get("email"));
        }
        
        if (profileData.containsKey("phone")) {
            currentUser.setPhone(profileData.get("phone"));
        }
        
        // 处理头像
        if (profileData.containsKey("avatar")) {
            String avatarUrl = profileData.get("avatar");
            logger.info("更新用户头像: {}", avatarUrl);
            
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                currentUser.setAvatar(avatarUrl);
            }
        }
        
        User updatedUser = userService.updateUser(currentUser);
        logger.info("用户资料已更新，头像URL: {}", updatedUser.getAvatar());
        
        return ResponseEntity.ok(ApiResponse.success("个人资料更新成功", updatedUser));
    }
    
    /**
     * 上传头像（直接上传并更新用户头像字段）
     */
    @PostMapping("/avatar")
    @AuditOperation(operation = "上传用户头像", operationType = "UPDATE", resourceType = "USER_AVATAR")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        User currentUser = authService.getCurrentUser();
        
        if (currentUser == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(400, "文件不能为空"));
            }
            
            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.ok(ApiResponse.error(400, "只支持图片文件上传"));
            }
            
            // 检查文件大小
            long fileSize = file.getSize();
            if (fileSize > 5 * 1024 * 1024) { // 5MB
                return ResponseEntity.ok(ApiResponse.error(400, "文件大小不能超过5MB"));
            }
            
            logger.info("开始上传头像，用户: {}", currentUser.getUsername());
            
            // 上传到指定目录
            Map<String, String> result = ossService.uploadFile(file, "avatar/");
            
            if (result != null && result.containsKey("url")) {
                String avatarUrl = result.get("url");
                logger.info("头像上传成功，URL: {}", avatarUrl);
                
                // 直接更新用户头像字段
                currentUser.setAvatar(avatarUrl);
                userService.updateUser(currentUser);
                
                logger.info("用户头像已更新到数据库");
            }
            
            return ResponseEntity.ok(ApiResponse.success("头像上传成功", result));
        } catch (Exception e) {
            logger.error("头像上传失败", e);
            return ResponseEntity.ok(ApiResponse.error(500, "头像上传失败: " + e.getMessage()));
        }
    }
} 