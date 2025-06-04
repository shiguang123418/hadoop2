package org.shiguang;

import org.shiguang.config.SparkContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * WebSocket应用程序主类
 * 用于启动WebSocket服务，提供实时数据监控功能
 */
@SpringBootApplication
@EnableScheduling
public class WebSocketApplication {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(WebSocketApplication.class, args);
    }

    /**
     * 在所有服务初始化完成后启动Spark Streaming Context
     */
    @Bean
    public CommandLineRunner initSparkStreaming(SparkContextManager sparkContextManager) {
        return args -> {
            try {
                logger.info("应用程序初始化完成，准备启动Spark Streaming...");
                // 启动Spark Streaming Context
                if (sparkContextManager.isInitialized()) {
                    sparkContextManager.startStreamingContext();
                    logger.info("Spark Streaming已启动");
                } else {
                    logger.warn("Spark上下文管理器未初始化，无法启动Spark Streaming");
                }
            } catch (Exception e) {
                logger.error("启动Spark Streaming时出错: {}", e.getMessage(), e);
            }
        };
    }
} 