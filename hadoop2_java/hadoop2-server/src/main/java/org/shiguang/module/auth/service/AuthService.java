package org.shiguang.module.auth.service;

import org.shiguang.entity.User;
import org.shiguang.entity.dto.LoginResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.Map;

/**
 * 认证服务接口
 */
public interface AuthService {
    
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 包含用户信息和令牌的响应DTO
     */
    LoginResponseDTO login(String username, String password);
    
    /**
     * 用户注册
     * @param user 用户信息
     * @return 创建的用户对象
     */
    User register(User user);
    
    /**
     * 创建JWT令牌
     * @param username 用户名
     * @return JWT令牌字符串
     */
    String createToken(String username);
    
    /**
     * 根据令牌获取认证信息
     * @param token JWT令牌
     * @return 认证信息
     */
    Authentication getAuthentication(String token);
    
    /**
     * 获取当前登录用户
     * @return 当前登录用户，如果未登录则返回null
     */
    User getCurrentUser();
    
    /**
     * 验证用户密码是否正确
     * @param username 用户名
     * @param password 密码
     * @return 密码是否正确
     */
    boolean validatePassword(String username, String password);
    
    /**
     * 检查当前用户是否具有指定角色
     * @param role 角色名称
     * @return 是否具有该角色
     */
    boolean hasRole(String role);
} 