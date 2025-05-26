package org.shiguang.module.common.security;

/**
 * 安全相关常量
 */
public final class SecurityConstants {
    /**
     * JWT令牌前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";
    
    /**
     * 认证请求头名称
     */
    public static final String HEADER_AUTH = "Authorization";
    
    /**
     * 用户角色
     */
    public static final String ROLE_USER = "ROLE_USER";
    
    /**
     * 管理员角色
     */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    
    /**
     * 公开路径
     */
    public static final String[] PUBLIC_PATHS = {
        "/auth/login",
        "/auth/register",
        "/swagger-ui.html",
        "/swagger-resources/**",
        "/webjars/**",
        "/v2/api-docs"
    };
    
    // 私有构造函数防止实例化
    private SecurityConstants() {
        throw new IllegalStateException("Utility class");
    }
} 