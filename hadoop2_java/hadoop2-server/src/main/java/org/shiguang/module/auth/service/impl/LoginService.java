package org.shiguang.module.auth.service.impl;

import org.shiguang.entity.User;
import org.shiguang.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 专门处理用户登录的服务
 * 提供增强的错误处理和密码验证功能
 */
@Service
public class LoginService {
    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthServiceImpl authService;

    @Autowired
    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthServiceImpl authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    /**
     * 执行用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     * @throws BadCredentialsException 如果用户名或密码错误
     * @throws IllegalStateException 如果账号已禁用或锁定
     * @throws RuntimeException 如果发生其他未预期的异常
     */
    public Map<String, Object> login(String username, String password) {
        // 验证输入
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        logger.info("正在验证用户登录: {}", username);
        
        // 查找用户
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (!userOptional.isPresent()) {
            logger.warn("登录失败: 用户不存在 - {}", username);
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        User user = userOptional.get();
        
        // 检查用户状态
        if (!"active".equals(user.getStatus())) {
            logger.warn("登录失败: 用户账号已禁用或锁定 - {}, 状态: {}", username, user.getStatus());
            throw new IllegalStateException("账号已被禁用或锁定");
        }
        
        // 验证密码 - 使用密码比较时间常数算法避免时序攻击
        boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
        if (!passwordMatches) {
            logger.warn("登录失败: 密码不匹配 - {}", username);
            throw new BadCredentialsException("用户名或密码错误");
        }
        
        logger.info("密码验证成功，正在生成令牌 - {}", username);
        
        try {
            // 更新最后登录时间
            user.setLastLoginAt(new Date());
            userRepository.save(user);
            
            // 生成JWT令牌
            String token = authService.createToken(username);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            
            // 用户信息（排除敏感字段）
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("name", user.getName());
            userInfo.put("phone", user.getPhone());
            
            // 确保头像URL正确返回
            if (user.getAvatar() != null && !user.getAvatar().trim().isEmpty()) {
                userInfo.put("avatar", user.getAvatar());
            } else {
                userInfo.put("avatar", null);
            }
            
            // 角色处理
            String role = user.getRole();
            if (role != null && !role.trim().isEmpty()) {
                // 确保角色格式正确
                if (!role.startsWith("ROLE_")) {
                    role = "ROLE_" + role.toUpperCase();
                }
                userInfo.put("role", role);
            } else {
                userInfo.put("role", "ROLE_USER");
            }
            
            userInfo.put("status", user.getStatus());
            userInfo.put("active", user.getActive());
            
            response.put("user", userInfo);
            
            logger.info("用户 {} 登录成功，生成令牌并返回用户信息", username);
            
            return response;
        } catch (Exception e) {
            logger.error("生成令牌或构建响应时发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("登录过程出现错误，请稍后再试");
        }
    }
} 