package org.shiguang.config;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spark上下文管理器
 * 用于在整个应用程序中管理单个SparkContext实例
 */
@Component
public class SparkContextManager {

    private static final Logger logger = LoggerFactory.getLogger(SparkContextManager.class);

    @Value("${spark.master:local[*]}")
    private String sparkMaster;

    @Value("${spark.batch.duration:5}")
    private int batchDuration;

    @Value("${spark.enabled:true}")
    private boolean sparkEnabled;

    private SparkConf sparkConf;
    private JavaSparkContext sparkContext;
    private SparkSession sparkSession;
    private JavaStreamingContext streamingContext;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        if (sparkEnabled && initialized.compareAndSet(false, true)) {
            try {
                logger.info("初始化Spark上下文管理器，master: {}, 批处理间隔: {} 秒", sparkMaster, batchDuration);

                // 创建Spark配置
                sparkConf = new SparkConf()
                        .setMaster(sparkMaster)
                        .setAppName("AgriDataProcessing")
                        .set("spark.streaming.stopGracefullyOnShutdown", "true");

                // 创建Streaming上下文，它会自动创建SparkContext
                streamingContext = new JavaStreamingContext(sparkConf, Durations.seconds(batchDuration));
                
                // 获取JavaSparkContext
                sparkContext = streamingContext.sparkContext();
                
                // 创建SparkSession
                sparkSession = SparkSession.builder()
                        .config(sparkContext.getConf())
                        .getOrCreate();
                
                logger.info("Spark上下文管理器初始化完成");
            } catch (Exception e) {
                initialized.set(false);
                logger.error("初始化Spark上下文管理器时出错: {}", e.getMessage(), e);
            }
        } else if (!sparkEnabled) {
            logger.info("Spark已禁用，不初始化Spark上下文");
        }
    }

    /**
     * 获取JavaSparkContext实例
     */
    public JavaSparkContext getSparkContext() {
        if (!initialized.get()) {
            logger.warn("尝试获取未初始化的SparkContext");
            return null;
        }
        return sparkContext;
    }

    /**
     * 获取SparkSession实例
     */
    public SparkSession getSparkSession() {
        if (!initialized.get()) {
            logger.warn("尝试获取未初始化的SparkSession");
            return null;
        }
        return sparkSession;
    }

    /**
     * 获取JavaStreamingContext实例
     */
    public JavaStreamingContext getStreamingContext() {
        if (!initialized.get()) {
            logger.warn("尝试获取未初始化的StreamingContext");
            return null;
        }
        return streamingContext;
    }

    /**
     * 启动StreamingContext
     */
    public void startStreamingContext() {
        if (initialized.get() && streamingContext != null) {
            try {
                logger.info("启动StreamingContext");
                streamingContext.start();
            } catch (Exception e) {
                logger.error("启动StreamingContext时出错: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * 关闭Spark资源
     */
    @PreDestroy
    public void shutdown() {
        if (initialized.compareAndSet(true, false)) {
            logger.info("关闭Spark上下文管理器");
            
            try {
                if (streamingContext != null) {
                    streamingContext.stop(true, true);
                    logger.info("StreamingContext已停止");
                }
            } catch (Exception e) {
                logger.error("停止StreamingContext时出错: {}", e.getMessage(), e);
            }
            
            try {
                if (sparkSession != null && !sparkSession.sparkContext().isStopped()) {
                    sparkSession.close();
                    logger.info("SparkSession已关闭");
                }
            } catch (Exception e) {
                logger.error("关闭SparkSession时出错: {}", e.getMessage(), e);
            }
            
            logger.info("Spark上下文管理器已关闭");
        }
    }
} 