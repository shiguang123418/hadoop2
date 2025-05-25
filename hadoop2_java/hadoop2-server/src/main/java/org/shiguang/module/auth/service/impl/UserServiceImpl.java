package org.shiguang.module.auth.service.impl;

import org.shiguang.entity.User;
import org.shiguang.repository.UserRepository;
import org.shiguang.module.auth.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 用户管理服务实现类
 */
@Service
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public List<User> getAllUsers() {
        try {
            return userRepository.findAll();
        } catch (Exception e) {
            logger.error("获取用户列表失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public User getUserById(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            return userOpt.orElse(null);
        } catch (Exception e) {
            logger.error("获取用户信息失败: " + userId, e);
            return null;
        }
    }

    @Override
    public User createUser(User user) {
        try {
            // 检查用户名是否已存在
            if (userRepository.existsByUsername(user.getUsername())) {
                throw new RuntimeException("用户名已存在");
            }

            // 加密密码
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            
            // 设置默认值
            if (user.getRole() == null || user.getRole().isEmpty()) {
                user.setRole("user");
            }
            
            if (user.getStatus() == null || user.getStatus().isEmpty()) {
                user.setStatus("active");
            }
            
            user.setCreatedAt(new Date());
            user.setUpdatedAt(new Date());
            
            return userRepository.save(user);
        } catch (Exception e) {
            logger.error("创建用户失败", e);
            throw new RuntimeException("创建用户失败: " + e.getMessage());
        }
    }

    @Override
    public User updateUser(User user) {
        try {
            // 检查用户是否存在
            Optional<User> existingUserOpt = userRepository.findById(user.getId());
            if (!existingUserOpt.isPresent()) {
                return null;
            }
            
            User existingUser = existingUserOpt.get();
            
            // 更新基本字段
            if (user.getName() != null) {
                existingUser.setName(user.getName());
            }
            
            if (user.getEmail() != null) {
                existingUser.setEmail(user.getEmail());
            }
            
            if (user.getPhone() != null) {
                existingUser.setPhone(user.getPhone());
            }
            
            // 更新头像URL
            if (user.getAvatar() != null) {
                logger.info("更新用户头像: " + user.getId() + ", 头像URL: " + user.getAvatar());
                existingUser.setAvatar(user.getAvatar());
            }
            
            // 更新密码（如果提供）
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            
            // 更新角色（如果提供）
            if (user.getRole() != null && !user.getRole().isEmpty()) {
                // 检查角色是否有效
                if (!"admin".equals(user.getRole()) && !"user".equals(user.getRole()) && !"guest".equals(user.getRole())) {
                    throw new RuntimeException("无效的角色值: " + user.getRole());
                }
                existingUser.setRole(user.getRole());
            }
            
            // 更新状态（如果提供）
            if (user.getStatus() != null && !user.getStatus().isEmpty()) {
                // 检查状态是否有效
                if (!"active".equals(user.getStatus()) && !"inactive".equals(user.getStatus()) && !"locked".equals(user.getStatus())) {
                    throw new RuntimeException("无效的状态值: " + user.getStatus());
                }
                existingUser.setStatus(user.getStatus());
            }
            
            existingUser.setUpdatedAt(new Date());
            
            return userRepository.save(existingUser);
        } catch (Exception e) {
            logger.error("更新用户失败: " + user.getId(), e);
            throw new RuntimeException("更新用户失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteUser(Long userId) {
        try {
            // 检查用户是否存在
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return false;
            }
            
            userRepository.deleteById(userId);
            return true;
        } catch (Exception e) {
            logger.error("删除用户失败: " + userId, e);
            throw new RuntimeException("删除用户失败: " + e.getMessage());
        }
    }

    @Override
    public boolean changePassword(Long userId, String newPassword) {
        try {
            // 检查用户是否存在
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return false;
            }
            
            User user = userOpt.get();
            
            // 加密新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(new Date());
            // 更新密码修改时间
            user.setPasswordLastChanged(new Date());
            
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            logger.error("更改用户密码失败: " + userId, e);
            throw new RuntimeException("更改用户密码失败: " + e.getMessage());
        }
    }

    @Override
    public User changeUserStatus(Long userId, String status) {
        try {
            // 检查用户是否存在
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return null;
            }
            
            User user = userOpt.get();
            
            // 检查状态是否有效
            if (!"active".equals(status) && !"inactive".equals(status) && !"locked".equals(status)) {
                throw new RuntimeException("无效的状态值: " + status);
            }
            
            user.setStatus(status);
            user.setUpdatedAt(new Date());
            
            return userRepository.save(user);
        } catch (Exception e) {
            logger.error("更改用户状态失败: " + userId, e);
            throw new RuntimeException("更改用户状态失败: " + e.getMessage());
        }
    }

    @Override
    public User changeUserRole(Long userId, String role) {
        try {
            // 检查用户是否存在
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return null;
            }
            
            User user = userOpt.get();
            
            // 检查角色是否有效
            if (!"admin".equals(role) && !"user".equals(role) && !"guest".equals(role)) {
                throw new RuntimeException("无效的角色值: " + role);
            }
            
            user.setRole(role);
            user.setUpdatedAt(new Date());
            
            return userRepository.save(user);
        } catch (Exception e) {
            logger.error("更改用户角色失败: " + userId, e);
            throw new RuntimeException("更改用户角色失败: " + e.getMessage());
        }
    }
} 