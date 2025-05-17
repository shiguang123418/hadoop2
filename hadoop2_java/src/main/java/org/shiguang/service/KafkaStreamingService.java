package org.shiguang.service;

import org.apache.spark.streaming.api.java.JavaStreamingContext;

/**
 * Kafka流处理服务接口
 */
public interface KafkaStreamingService {
    
    /**
     * 启动Kafka数据流处理
     * 
     * @param topics 需要订阅的Kafka主题数组
     * @return 是否成功启动
     */
    boolean startStreamProcessing(String[] topics);
    
    /**
     * 停止Kafka数据流处理
     * 
     * @return 是否成功停止
     */
    boolean stopStreamProcessing();
    
    /**
     * 获取Streaming处理的状态
     * 
     * @return 是否正在运行中
     */
    boolean isStreamingActive();
    
    /**
     * 获取当前的JavaStreamingContext
     * 
     * @return JavaStreamingContext实例，如果未初始化或已停止则返回null
     */
    JavaStreamingContext getStreamingContext();
}