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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

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
            logger.error("头像上传失败: 用户未登录");
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        
        try {
            if (file.isEmpty()) {
                logger.error("头像上传失败: 文件为空");
                return ResponseEntity.ok(ApiResponse.error(400, "文件不能为空"));
            }
            
            String originalFilename = file.getOriginalFilename();
            long fileSize = file.getSize();
            String contentType = file.getContentType();
            
            // 检查文件类型
            if (contentType == null || !contentType.startsWith("image/")) {
                logger.error("头像上传失败: 不支持的文件类型 {}", contentType);
                return ResponseEntity.ok(ApiResponse.error(400, "只支持图片文件上传"));
            }
            
            // 检查文件大小
            if (fileSize > 5 * 1024 * 1024) { // 5MB
                logger.error("头像上传失败: 文件大小超过限制 {} bytes", fileSize);
                return ResponseEntity.ok(ApiResponse.error(400, "文件大小不能超过5MB"));
            }
            
            logger.info("开始上传头像 - 用户: {}, 文件名: {}, 大小: {} bytes, 类型: {}", 
                    currentUser.getUsername(), originalFilename, fileSize, contentType);
            
            try {
                // 上传到指定目录
                Map<String, String> result = ossService.uploadFile(file, "avatar/");
                logger.info("OSS上传成功 - 结果: {}", result);
                
                if (result == null) {
                    logger.error("OSS上传失败 - 返回结果为null");
                    return ResponseEntity.ok(ApiResponse.error(500, "头像上传失败: OSS服务返回为空"));
                }
                
                String avatarUrl = null;
                
                // 尝试从不同字段获取URL
                if (result.containsKey("url")) {
                    avatarUrl = result.get("url");
                    logger.info("从url字段获取头像URL: {}", avatarUrl);
                } else if (result.containsKey("path")) {
                    avatarUrl = result.get("path");
                    logger.info("从path字段获取头像URL: {}", avatarUrl);
                } else if (result.containsKey("ossUrl")) {
                    avatarUrl = result.get("ossUrl");
                    logger.info("从ossUrl字段获取头像URL: {}", avatarUrl);
                } else {
                    // 构造URL
                    String bucketName = result.getOrDefault("bucketName", "shiguang123418");
                    String filename = result.getOrDefault("filename", null);
                    String objectKey = result.getOrDefault("objectKey", null);
                    
                    if (objectKey != null) {
                        avatarUrl = "https://" + bucketName + ".oss-cn-hangzhou.aliyuncs.com/" + objectKey;
                        logger.info("根据objectKey构造头像URL: {}", avatarUrl);
                    } else if (filename != null) {
                        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
                        avatarUrl = "https://" + bucketName + ".oss-cn-hangzhou.aliyuncs.com/avatar/" + datePath + "/" + filename;
                        logger.info("根据filename构造头像URL: {}", avatarUrl);
                    }
                }
                
                // 更新用户头像字段
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    try {
                        // 使用专门的安全方法更新头像，确保不影响其他字段特别是密码
                        User updatedUser = userService.updateUserAvatar(currentUser.getId(), avatarUrl);
                        
                        if (updatedUser == null) {
                            logger.error("更新头像失败: 用户不存在或更新过程出错");
                            return ResponseEntity.ok(ApiResponse.error(404, "更新头像失败，用户不存在"));
                        }
                        
                        logger.info("用户头像已安全更新到数据库: {}", avatarUrl);
                        
                        // 确保result中包含url字段
                        result.put("url", avatarUrl);
                    } catch (Exception e) {
                        logger.error("更新用户头像失败: {}", e.getMessage(), e);
                        return ResponseEntity.ok(ApiResponse.error(500, "头像上传成功，但更新用户失败: " + e.getMessage()));
                    }
                } else {
                    logger.error("未能从OSS结果中提取头像URL: {}", result);
                    return ResponseEntity.ok(ApiResponse.error(500, "头像上传失败: 无法获取URL"));
                }
                
                return ResponseEntity.ok(ApiResponse.success("头像上传成功", result));
            } catch (Exception ossEx) {
                logger.error("OSS上传过程出错: {}", ossEx.getMessage(), ossEx);
                return ResponseEntity.ok(ApiResponse.error(500, "头像上传过程出错: " + ossEx.getMessage()));
            }
        } catch (Exception e) {
            logger.error("头像上传处理过程出现异常: {}", e.getMessage(), e);
            if (e.getCause() != null) {
                logger.error("根本原因: {}", e.getCause().getMessage(), e.getCause());
            }
            return ResponseEntity.ok(ApiResponse.error(500, "头像上传失败: " + e.getMessage()));
        }
    }
} 