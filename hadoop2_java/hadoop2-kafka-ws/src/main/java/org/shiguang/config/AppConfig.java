package org.shiguang.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring配置类，用于获取application.yml中的配置
 */
@Configuration
public class AppConfig {
    
    @Autowired
    private Environment env;

    /**
     * 获取字符串属性
     * @param path 属性路径
     * @param defaultValue 默认值
     * @return 属性值或默认值
     */
    public String getString(String path, String defaultValue) {
        return env.getProperty(path, defaultValue);
    }

    /**
     * 获取整数属性
     * @param path 属性路径
     * @param defaultValue 默认值
     * @return 属性值或默认值
     */
    public int getInt(String path, int defaultValue) {
        return env.getProperty(path, Integer.class, defaultValue);
    }

    /**
     * 获取双精度浮点数属性
     * @param path 属性路径
     * @param defaultValue 默认值
     * @return 属性值或默认值
     */
    public double getDouble(String path, double defaultValue) {
        return env.getProperty(path, Double.class, defaultValue);
    }

    /**
     * 获取布尔属性
     * @param path 属性路径
     * @param defaultValue 默认值
     * @return 属性值或默认值
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        return env.getProperty(path, Boolean.class, defaultValue);
    }
} 