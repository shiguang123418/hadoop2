package org.shiguang.config;

import com.typesafe.config.Config;
import org.shiguang.utils.ConfigManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Typesafe Config适配器
 * 将自定义配置系统与Spring Boot配置系统整合
 */
@Configuration
public class TypesafeConfigAdapter {

    @Bean
    public Config typesafeConfig() {
        return ConfigManager.loadConfig();
    }
} 