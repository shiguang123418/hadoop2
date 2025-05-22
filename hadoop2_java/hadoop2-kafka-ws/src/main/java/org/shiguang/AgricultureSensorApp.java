package org.shiguang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.shiguang.config.AppProperties;

/**
 * 农业传感器数据处理应用程序
 * Spring Boot主应用类
 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class AgricultureSensorApp {
    
    public static void main(String[] args) {
        SpringApplication.run(AgricultureSensorApp.class, args);
    }
} 