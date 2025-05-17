package org.shiguang.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 配置文件信息配置类
 * 用于获取当前激活的配置文件信息
 */
@Component
public class ProfileConfig {

    @Autowired
    private Environment environment;

    /**
     * 获取当前激活的配置文件名称
     * 如果有多个激活的配置文件，则返回第一个
     * 如果没有激活的配置文件，则返回默认配置文件
     */
    public String getActiveProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            return activeProfiles[0];
        }
        
        String[] defaultProfiles = environment.getDefaultProfiles();
        return defaultProfiles.length > 0 ? defaultProfiles[0] : "default";
    }
    
    /**
     * 判断指定的配置文件是否被激活
     */
    public boolean isProfileActive(String profileName) {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if (profile.equals(profileName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取应用名称
     */
    public String getApplicationName() {
        return environment.getProperty("spring.application.name", "农业大数据平台");
    }
    
    /**
     * 获取应用版本
     */
    public String getApplicationVersion() {
        return environment.getProperty("application.version", "1.0.0");
    }
} 