package org.shiguang.common.security;

/**
 * 安全常量类 - 定义角色和权限
 */
public final class SecurityConstants {
    // 角色常量
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_HDFS = "ROLE_HDFS";
    public static final String ROLE_HIVE = "ROLE_HIVE";
    
    // 权限前缀
    public static final String ROLE_PREFIX = "ROLE_";
    
    // 认证相关常量
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_AUTH = "Authorization";
    
    // 权限常量
    public static final String PERM_READ = "READ";
    public static final String PERM_WRITE = "WRITE";
    public static final String PERM_EXECUTE = "EXECUTE";
    public static final String PERM_ADMIN = "ADMIN";
    
    private SecurityConstants() {
        // 防止实例化
    }
} 