package org.shiguang.utils;

/**
 * 应用程序常量
 */
public final class AppConstants {
    
    // 系统相关常量
    public static final String APP_NAME = "Hadoop数据服务平台";
    public static final String API_VERSION = "v1";
    public static final String API_BASE_PATH = "/api/" + API_VERSION;
    
    // 权限相关常量
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_AUTH = "Authorization";
    public static final long TOKEN_EXPIRATION = 24 * 60 * 60 * 1000; // 24小时(毫秒)
    
    // HDFS相关常量
    public static final String HDFS_DEFAULT_USER = "root";
    public static final String HDFS_DEFAULT_PATH = "/user/hadoop/upload";
    public static final int HDFS_BUFFER_SIZE = 4 * 1024 * 1024;
    
    // Hive相关常量
    public static final String HIVE_DEFAULT_DATABASE = "default";
    public static final int HIVE_CONNECTION_TIMEOUT = 30; // 秒
    
    /**
     * 最大文件上传大小 (100MB)
     */
    public static final long MAX_FILE_SIZE = 100 * 1024 * 1024;
    
    /**
     * API成功状态码
     */
    public static final int API_SUCCESS_CODE = 200;
    
    /**
     * API错误状态码
     */
    public static final int API_ERROR_CODE = 500;
    
    /**
     * 默认分页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 10;
    
    // 私有构造函数防止实例化
    private AppConstants() {
        throw new IllegalStateException("Utility class");
    }
} 