package org.shiguang.spark;

import org.shiguang.config.AppConfig;
import org.shiguang.spark.processor.SensorProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.Serializable;

/**
 * Spark作业运行器
 * 在JVM内部直接运行Spark处理任务，使用Kryo序列化器提高性能和解决序列化问题
 */
@Primary
@Component
public class SparkJobRunner implements SensorProcessor, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SparkJobRunner.class);
    
    @Autowired
    private AppConfig config;
    
    private Thread sparkThread;
    private volatile boolean isRunning = false;
    private volatile boolean stopRequested = false;
    
    @Override
    public void init() {
        logger.info("初始化Spark作业运行器");
        // 基础初始化，实际配置在运行时进行
    }
    
    @Override
    public synchronized boolean start() {
        if (isRunning) {
            logger.warn("Spark作业已经在运行中，无法再次启动");
            return false;
        }
        
        try {
            // 获取配置项
            final String kafkaServers = config.getString("kafka.bootstrap.servers", "localhost:9092");
            final String kafkaTopic = config.getString("kafka.topic", "agriculture-sensor-data");
            final String webSocketUrl = "ws://" + 
                    config.getString("websocket.host", "localhost") + ":" + 
                    config.getInt("websocket.port", 8090);
            
            logger.info("使用Kryo序列化器直接在内部启动Spark作业，参数: kafkaServers={}, topic={}, webSocketUrl={}", 
                    kafkaServers, kafkaTopic, webSocketUrl);
            
            // 创建并启动新线程来运行Spark作业
            sparkThread = new Thread(() -> {
                try {
                    // 创建SparkSubmitJob实例
                    SparkSubmitJob job = new SparkSubmitJob();
                    
                    // 读取并设置Spark相关配置
                    String master = config.getString("spark.master", "local[2]");
                    String appName = config.getString("spark.app.name", "AgricultureSensorProcessor");
                    boolean useKryo = config.getBoolean("spark.kryo.enabled", true);
                    int kryoBufferMaxSize = config.getInt("spark.kryoserializer.buffer.max.mb", 64);
                    
                    // 直接调用SparkSubmitJob的run方法，传递Kryo相关配置
                    job.run(kafkaServers, kafkaTopic, webSocketUrl, master, appName, useKryo, kryoBufferMaxSize);
                    
                } catch (Exception e) {
                    logger.error("Spark作业运行出错", e);
                } finally {
                    if (!stopRequested) {
                        logger.info("Spark作业已退出");
                    }
                    isRunning = false;
                }
            }, "SparkJobThread");
            
            // 设置为守护线程，使得应用程序可以正常退出
            sparkThread.setDaemon(true);
            sparkThread.start();
            
            isRunning = true;
            stopRequested = false;
            logger.info("使用Kryo序列化器的Spark作业已启动");
            return true;
        } catch (Exception e) {
            logger.error("启动Spark作业失败", e);
            return false;
        }
    }
    
    @Override
    @PreDestroy
    public synchronized void stop() {
        if (!isRunning || sparkThread == null) {
            logger.info("Spark作业未在运行，无需停止");
            return;
        }
        
        logger.info("正在停止Spark作业...");
        
        try {
            // 标记停止请求
            stopRequested = true;
            
            // 中断线程
            sparkThread.interrupt();
            
            // 等待线程结束
            sparkThread.join(10000); // 等待最多10秒
            
            if (sparkThread.isAlive()) {
                logger.warn("Spark作业线程未能正常停止");
            } else {
                logger.info("Spark作业已停止");
            }
        } catch (Exception e) {
            logger.error("停止Spark作业时出错", e);
        } finally {
            isRunning = false;
            sparkThread = null;
        }
    }
    
    @Override
    public boolean isRunning() {
        return isRunning && sparkThread != null && sparkThread.isAlive();
    }
} 