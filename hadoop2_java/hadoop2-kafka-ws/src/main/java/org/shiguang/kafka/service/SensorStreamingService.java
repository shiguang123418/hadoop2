package org.shiguang.kafka.service;

import org.shiguang.spark.processor.SensorProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

/**
 * 传感器流处理服务，管理SensorProcessor的生命周期
 * <p>
 * 该服务负责控制传感器数据流处理的启动和停止，并提供流处理状态的查询接口。
 * 它在应用程序启动时自动启动流处理，也可以通过REST API手动控制。
 * </p>
 */
@Service
public class SensorStreamingService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(SensorStreamingService.class);
    
    @Autowired
    private SensorProcessor processor;

    /**
     * 当Spring Boot应用启动时自动运行
     * @param args 命令行参数
     */
    @Override
    public void run(ApplicationArguments args) {
        logger.info("正在初始化农业传感器数据处理服务...");
        
        // 检查是否有启动参数指定不自动启动流处理
        if (args.containsOption("no-autostart")) {
            logger.info("检测到--no-autostart参数，流处理服务将不会自动启动");
            return;
        }
        
        // 自动启动流处理
        startStreaming();
    }

    /**
     * 启动传感器流处理
     * @return 如果成功启动则为true
     */
    public synchronized boolean startStreaming() {
        try {
            if (!processor.isRunning()) {
                logger.info("正在启动传感器流处理服务...");
                boolean started = processor.start();
                if (started) {
                    logger.info("传感器流处理服务启动成功");
                } else {
                    logger.warn("传感器流处理服务无法启动");
                }
                return started;
            } else {
                logger.warn("传感器流处理服务已经在运行");
                return false;
            }
        } catch (Exception e) {
            logger.error("启动传感器流处理服务失败", e);
            return false;
        }
    }

    /**
     * 停止传感器流处理
     * @return 如果成功停止则为true
     */
    public synchronized boolean stopStreaming() {
        try {
            if (processor.isRunning()) {
                logger.info("正在停止传感器流处理服务...");
                processor.stop();
                logger.info("传感器流处理服务停止成功");
                return true;
            } else {
                logger.warn("传感器流处理服务未在运行");
                return false;
            }
        } catch (Exception e) {
            logger.error("停止传感器流处理服务失败", e);
            return false;
        }
    }

    /**
     * 检查流处理服务是否正在运行
     * @return 如果正在运行则为true
     */
    public boolean isRunning() {
        return processor.isRunning();
    }
} 