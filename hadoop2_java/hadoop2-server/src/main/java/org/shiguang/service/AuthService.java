package org.shiguang.service;

import org.shiguang.entity.User;
import org.springframework.security.core.Authentication;

import java.util.Map;

public interface AuthService {
    
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 包含token和用户信息的响应
     */
    Map<String, Object> login(String username, String password);
    
    /**
     * 注册新用户
     * @param user 用户信息
     * @return 创建的用户
     */
    User register(User user);
    
    /**
     * 生成JWT令牌
     * @param username 用户名
     * @return JWT令牌
     */
    String createToken(String username);
    
    /**
     * 验证JWT令牌并获取认证信息
     * @param token JWT令牌
     * @return Spring Security认证对象
     */
    Authentication getAuthentication(String token);
    
    /**
     * 获取当前认证用户
     * @return 当前用户
     */
    User getCurrentUser();
} 