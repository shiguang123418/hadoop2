package org.shiguang.kafka.service;

import org.shiguang.spark.AgricultureSensorProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

/**
 * 传感器流处理服务，管理AgricultureSensorProcessor的生命周期
 */
@Service
public class SensorStreamingService implements ApplicationRunner {

    @Autowired
    private AgricultureSensorProcessor processor;

    /**
     * 当Spring Boot应用启动时自动运行
     * @param args 命令行参数
     */
    @Override
    public void run(ApplicationArguments args) {
        System.out.println("正在启动农业传感器数据处理服务...");
        startStreaming();
    }

    /**
     * 启动传感器流处理
     * @return 如果成功启动则为true
     */
    public synchronized boolean startStreaming() {
        if (!processor.isRunning()) {
            boolean started = processor.start();
            if (started) {
                System.out.println("传感器流处理服务启动成功");
            } else {
                System.out.println("传感器流处理服务已经在运行或无法启动");
            }
            return started;
        } else {
            System.out.println("传感器流处理服务已经在运行");
            return false;
        }
    }

    /**
     * 停止传感器流处理
     * @return 如果成功停止则为true
     */
    public synchronized boolean stopStreaming() {
        if (processor.isRunning()) {
            processor.stop();
            System.out.println("传感器流处理服务停止成功");
            return true;
        } else {
            System.out.println("传感器流处理服务未在运行");
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