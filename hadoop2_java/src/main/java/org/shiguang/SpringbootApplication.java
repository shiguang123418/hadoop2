package org.shiguang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import lombok.extern.slf4j.Slf4j;

/**
 * 农业大数据平台应用
 */
@SpringBootApplication
@Slf4j
public class SpringbootApplication {

    public static void main(String[] args) {
        // 启动应用
        ConfigurableApplicationContext context = SpringApplication.run(SpringbootApplication.class, args);
        
        // 获取环境信息
        Environment env = context.getEnvironment();
        String[] activeProfiles = env.getActiveProfiles();
        
        // 打印激活的配置文件
        if (activeProfiles.length > 0) {
            log.info("应用启动成功，激活的配置文件: {}", String.join(", ", activeProfiles));
        } else {
            log.info("应用启动成功，使用默认配置文件");
        }
        
        // 打印数据库连接信息（不包含密码）
        String dbUrl = env.getProperty("spring.datasource.url");
        if (dbUrl != null) {
            log.info("数据库连接: {}", dbUrl);
        }
    }
} 