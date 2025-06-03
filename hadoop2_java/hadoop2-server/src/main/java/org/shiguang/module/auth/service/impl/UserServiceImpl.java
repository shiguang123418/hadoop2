package org.shiguang.module.auth.service.impl;

import org.shiguang.entity.User;
import org.shiguang.repository.UserRepository;
import org.shiguang.module.auth.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
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
    
    @PersistenceContext
    private EntityManager entityManager;

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
            
            // 设置默认值并标准化角色名
            if (user.getRole() != null && !user.getRole().isEmpty()) {
                // 如果角色名包含ROLE_前缀，移除它
                String role = user.getRole();
                if (role.startsWith("ROLE_")) {
                    role = role.substring(5);
                }
                // 统一使用小写角色名
                user.setRole(role.toLowerCase());
            } else {
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
            logger.info("开始更新用户 ID: {}", user.getId());
            
            // 检查用户是否存在
            Optional<User> existingUserOpt = userRepository.findById(user.getId());
            if (!existingUserOpt.isPresent()) {
                logger.warn("用户不存在: {}", user.getId());
                return null;
            }
            
            User existingUser = existingUserOpt.get();
            
            // 记录更新前的密码哈希
            String originalPasswordHash = existingUser.getPassword();
            Date originalPasswordLastChanged = existingUser.getPasswordLastChanged();
            
            logger.info("更新用户前: ID={}, 用户名={}, 密码哈希={}", 
                    existingUser.getId(), 
                    existingUser.getUsername(), 
                    originalPasswordHash != null ? (originalPasswordHash.substring(0, 10) + "...") : "未设置");
            
            // 不处理密码字段，使用简单的手工复制方法，确保密码不会被清空
            if (user.getName() != null) {
                logger.info("更新用户名称: {}", user.getName());
                existingUser.setName(user.getName());
            }
            
            if (user.getEmail() != null) {
                logger.info("更新用户邮箱: {}", user.getEmail());
                existingUser.setEmail(user.getEmail());
            }
            
            if (user.getPhone() != null) {
                logger.info("更新用户电话: {}", user.getPhone());
                existingUser.setPhone(user.getPhone());
            }
            
            // 更新头像URL
            if (user.getAvatar() != null) {
                logger.info("更新用户头像: {}", user.getAvatar());
                existingUser.setAvatar(user.getAvatar());
            }
            
            // 仅当明确传递了非空密码时才更新密码
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                // 如果直接提供了加密后的密码哈希（通常来自数据库查询）
                if (user.getPassword().startsWith("$2a$") && user.getPassword().length() > 50) {
                    logger.info("检测到直接使用密码哈希更新");
                    existingUser.setPassword(user.getPassword());
                } else {
                    // 否则对密码进行加密
                    logger.info("对新密码进行加密");
                    existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
                    existingUser.setPasswordLastChanged(new Date());
                }
            } else {
                // 重要：确保密码和密码修改时间不会被空值覆盖
                logger.info("保留原密码哈希不变");
                existingUser.setPassword(originalPasswordHash);
                existingUser.setPasswordLastChanged(originalPasswordLastChanged);
            }
            
            // 更新角色（如果提供）
            if (user.getRole() != null && !user.getRole().isEmpty()) {
                logger.info("更新用户角色: {}", user.getRole());
                existingUser.setRole(user.getRole());
            }
            
            // 更新状态（如果提供）
            if (user.getStatus() != null && !user.getStatus().isEmpty()) {
                logger.info("更新用户状态: {}", user.getStatus());
                existingUser.setStatus(user.getStatus());
            }
            
            // 强制设置更新时间
            existingUser.setUpdatedAt(new Date());
            
            // 保存前再次确认密码没有被修改
            if (!originalPasswordHash.equals(existingUser.getPassword())) {
                // 如果此时密码与原密码不同，但没有明确要求更改密码，则恢复原密码
                if (user.getPassword() == null || user.getPassword().isEmpty()) {
                    logger.warn("检测到密码被意外修改，恢复为原密码");
                    existingUser.setPassword(originalPasswordHash);
                    existingUser.setPasswordLastChanged(originalPasswordLastChanged);
                }
            }
            
            logger.info("保存用户前: ID={}, 用户名={}, 密码状态={}", 
                    existingUser.getId(), 
                    existingUser.getUsername(),
                    existingUser.getPassword() != null ? "已设置" : "未设置");
            
            User savedUser = userRepository.save(existingUser);
            
            // 验证保存后密码是否正确保留
            if (!originalPasswordHash.equals(savedUser.getPassword()) && 
                (user.getPassword() == null || user.getPassword().isEmpty())) {
                logger.error("严重错误：尽管尝试保留原密码，但保存后密码仍被修改");
            } else {
                logger.info("用户更新成功，密码状态正常");
            }
            
            return savedUser;
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
            
            // 如果角色名包含ROLE_前缀，移除它
            if (role.startsWith("ROLE_")) {
                role = role.substring(5);
            }
            
            // 转换为小写进行比较
            String roleLower = role.toLowerCase();
            
            // 检查角色是否有效
            if (!"admin".equals(roleLower) && !"user".equals(roleLower) && !"guest".equals(roleLower)) {
                throw new RuntimeException("无效的角色值: " + role);
            }
            
            // 保存小写形式的角色名
            user.setRole(roleLower);
            user.setUpdatedAt(new Date());
            
            return userRepository.save(user);
        } catch (Exception e) {
            logger.error("更改用户角色失败: " + userId, e);
            throw new RuntimeException("更改用户角色失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public User updateUserAvatar(Long userId, String avatarUrl) {
        try {
            logger.info("开始安全更新用户头像: 用户ID={}, 头像URL={}", userId, avatarUrl);
            
            // 直接使用原生SQL更新头像字段，避免JPA影响其他字段
            // 这是最安全的方式，不经过JPA实体更新，只修改头像字段
            int updatedRows = 0;
            
            try {
                // 使用注入的EntityManager执行原生SQL
                Query query = entityManager.createNativeQuery(
                        "UPDATE users SET avatar = :avatarUrl, updated_at = :updatedAt " +
                        "WHERE id = :userId"
                );
                query.setParameter("avatarUrl", avatarUrl);
                query.setParameter("updatedAt", new Date());
                query.setParameter("userId", userId);
                
                updatedRows = query.executeUpdate();
                logger.info("直接SQL更新头像成功，影响行数: {}", updatedRows);
            } catch (Exception e) {
                logger.error("执行原生SQL更新头像失败", e);
                // 如果原生SQL失败，回退到JPA方式
            }
            
            // 如果原生SQL方式更新失败，使用JPA的方式更新，但要特别小心
            if (updatedRows == 0) {
                logger.info("回退使用JPA方式更新头像");
                
                // 获取完整用户对象
                Optional<User> userOpt = userRepository.findById(userId);
                if (!userOpt.isPresent()) {
                    logger.warn("用户不存在: {}", userId);
                    return null;
                }
                
                User existingUser = userOpt.get();
                String originalPassword = existingUser.getPassword();
                Date originalPasswordLastChanged = existingUser.getPasswordLastChanged();
                
                // 只更新头像
                existingUser.setAvatar(avatarUrl);
                existingUser.setUpdatedAt(new Date());
                
                // 保存前确认密码没变
                if (existingUser.getPassword() == null || 
                    !existingUser.getPassword().equals(originalPassword)) {
                    logger.warn("检测到密码被意外修改，恢复原密码");
                    existingUser.setPassword(originalPassword);
                    existingUser.setPasswordLastChanged(originalPasswordLastChanged);
                }
                
                User savedUser = userRepository.save(existingUser);
                
                // 保存后验证密码
                if (!savedUser.getPassword().equals(originalPassword)) {
                    logger.error("严重错误：头像更新后密码仍被修改：原始密码={}, 新密码={}", 
                              originalPassword != null ? "存在" : "不存在", 
                              savedUser.getPassword() != null ? "存在" : "不存在");
                } else {
                    logger.info("JPA方式更新头像成功，密码保持不变");
                }
                
                return savedUser;
            } else {
                // 原生SQL成功更新，重新加载用户对象
                return getUserById(userId);
            }
        } catch (Exception e) {
            logger.error("更新用户头像失败: " + userId, e);
            throw new RuntimeException("更新用户头像失败: " + e.getMessage());
        }
    }
} 