package org.shiguang.utils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置管理类，提供统一的配置访问和管理功能
 */
public class ConfigManager {
    private static Config configInstance;
    
    // 配置文件名
    private static final String CONFIG_FILENAME = "application.conf";
    
    // 环境变量配置
    private static final String ENV_CONFIG_PATH = "CONFIG_PATH";
    private static final String ENV_ACTIVE_PROFILE = "ACTIVE_PROFILE";
    
    /**
     * 加载配置文件
     * 优先级：
     * 1. 指定的环境变量路径
     * 2. 指定的系统属性路径
     * 3. 项目资源目录下的配置
     */
    public static synchronized Config loadConfig() {
        if (configInstance == null) {
            Config config = loadConfigFromSources();
            configInstance = config;
            
            // 打印配置信息（仅用于调试）
            System.out.println("配置加载完成，当前激活的环境: " + getActiveProfile());
        }
        
        return configInstance;
    }
    
    /**
     * 从各种来源加载配置
     */
    private static Config loadConfigFromSources() {
        // 1. 检查环境变量中指定的配置文件
        String envConfigPath = System.getenv(ENV_CONFIG_PATH);
        Config envConfig = null;
        
        if (envConfigPath != null) {
            envConfig = loadConfigFromFile(new File(envConfigPath));
        }
        
        // 2. 从类路径加载配置
        Config classPathConfig = ConfigFactory.load(CONFIG_FILENAME);
        
        // 3. 合并配置（外部配置优先级高于内部配置）
        Config combinedConfig = (envConfig != null) 
            ? envConfig.withFallback(classPathConfig) 
            : classPathConfig;
        
        // 4. 替换占位符并应用环境特定的配置
        Config resolvedConfig = applyProfileSpecificConfig(combinedConfig);
        
        return resolvedConfig;
    }
    
    /**
     * 从文件加载配置
     */
    private static Config loadConfigFromFile(File file) {
        if (file.exists() && file.isFile()) {
            try {
                return ConfigFactory.parseFile(file);
            } catch (Exception e) {
                System.out.println("从文件加载配置失败: " + file.getAbsolutePath() + ", 错误: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        } else {
            System.out.println("配置文件不存在: " + file.getAbsolutePath());
            return null;
        }
    }
    
    /**
     * 应用环境特定的配置
     */
    private static Config applyProfileSpecificConfig(Config baseConfig) {
        String profile = getActiveProfile();
        String profileConfig = "profiles." + profile;
        
        if (baseConfig.hasPath(profileConfig)) {
            // 如果存在特定环境的配置，将其与基础配置合并
            Config envSpecificConfig = baseConfig.getConfig(profileConfig);
            return envSpecificConfig.withFallback(baseConfig).resolve();
        } else {
            return baseConfig.resolve();
        }
    }
    
    /**
     * 获取当前激活的环境
     */
    public static String getActiveProfile() {
        String env = System.getenv(ENV_ACTIVE_PROFILE);
        if (env != null) {
            return env;
        }
        
        String prop = System.getProperty(ENV_ACTIVE_PROFILE);
        if (prop != null) {
            return prop;
        }
        
        return "dev"; // 默认为开发环境
    }
    
    /**
     * 获取字符串配置项
     */
    public static String getString(String path, String defaultValue) {
        Config config = loadConfig();
        return config.hasPath(path) ? config.getString(path) : defaultValue;
    }
    
    public static String getString(String path) {
        return getString(path, "");
    }
    
    /**
     * 获取整数配置项
     */
    public static int getInt(String path, int defaultValue) {
        Config config = loadConfig();
        return config.hasPath(path) ? config.getInt(path) : defaultValue;
    }
    
    public static int getInt(String path) {
        return getInt(path, 0);
    }
    
    /**
     * 获取长整型配置项
     */
    public static long getLong(String path, long defaultValue) {
        Config config = loadConfig();
        return config.hasPath(path) ? config.getLong(path) : defaultValue;
    }
    
    public static long getLong(String path) {
        return getLong(path, 0L);
    }
    
    /**
     * 获取布尔型配置项
     */
    public static boolean getBoolean(String path, boolean defaultValue) {
        Config config = loadConfig();
        return config.hasPath(path) ? config.getBoolean(path) : defaultValue;
    }
    
    public static boolean getBoolean(String path) {
        return getBoolean(path, false);
    }
    
    /**
     * 获取双精度浮点型配置项
     */
    public static double getDouble(String path, double defaultValue) {
        Config config = loadConfig();
        return config.hasPath(path) ? config.getDouble(path) : defaultValue;
    }
    
    public static double getDouble(String path) {
        return getDouble(path, 0.0);
    }
    
    /**
     * 获取持续时间配置项（例如 5s, 10m 等）
     */
    public static Duration getDuration(String path, Duration defaultValue) {
        Config config = loadConfig();
        return config.hasPath(path) ? config.getDuration(path) : defaultValue;
    }
    
    public static Duration getDuration(String path) {
        return getDuration(path, Duration.ZERO);
    }
    
    /**
     * 获取字符串列表配置项
     */
    public static List<String> getStringList(String path) {
        Config config = loadConfig();
        if (config.hasPath(path)) {
            return config.getStringList(path);
        } else {
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取子配置
     */
    public static Config getConfig(String path) {
        Config config = loadConfig();
        return config.hasPath(path) ? config.getConfig(path) : null;
    }
} 