package org.shiguang.utils;

import org.shiguang.module.kafka.client.KafkaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据推送工具类
 * 提供将CSV文件数据推送到Kafka的功能
 */
@Component
public class DataPushUtil {
    private static final Logger logger = LoggerFactory.getLogger(DataPushUtil.class);
    
    @Autowired
    private KafkaClient kafkaClient;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> scheduledTask;
    
    /**
     * 以流模式按行推送CSV文件内容到Kafka
     * 定期发送数据，模拟实时数据流
     * 
     * @param filePath CSV文件路径
     * @param topicName Kafka主题
     * @param intervalMs 发送间隔(毫秒)
     * @param repeat 是否循环发送
     * @return 操作状态描述
     */
    public String streamCsvToKafka(String filePath, String topicName, long intervalMs, boolean repeat) {
        try {
            // 读取文件全部内容
            List<String> allLines = Files.readAllLines(Paths.get(filePath));
            if (allLines.isEmpty()) {
                return "文件为空";
            }
            
            // 提取标题行
            final String header = allLines.get(0);
            // 准备数据行
            final List<String> dataLines = allLines.subList(1, allLines.size());
            if (dataLines.isEmpty()) {
                return "文件只有标题行，无数据";
            }
            
            // 发送标题行
            kafkaClient.sendMessage(topicName, "header", header);
            logger.info("已发送标题行到Kafka主题: {}", topicName);
            
            // 创建索引计数器
            final AtomicInteger index = new AtomicInteger(0);
            final int totalLines = dataLines.size();
            
            // 停止之前可能存在的任务
            if (scheduledTask != null && !scheduledTask.isDone()) {
                scheduledTask.cancel(false);
            }
            
            // 定期发送数据任务
            scheduledTask = scheduler.scheduleAtFixedRate(() -> {
                try {
                    int currentIndex = index.getAndIncrement();
                    
                    // 检查是否需要重置索引（循环模式）
                    if (currentIndex >= totalLines) {
                        if (repeat) {
                            index.set(0);
                            currentIndex = 0;
                            logger.info("数据发送完成，重新开始循环发送");
                        } else {
                            logger.info("数据发送完成，不再循环");
                            if (scheduledTask != null) {
                                scheduledTask.cancel(false);
                            }
                            return;
                        }
                    }
                    
                    // 发送当前行
                    String line = dataLines.get(currentIndex);
                    kafkaClient.sendMessage(topicName, String.valueOf(currentIndex), line);
                    
                    if (currentIndex % 100 == 0 || currentIndex == totalLines - 1) {
                        logger.info("已发送 {}/{} 行数据到Kafka主题: {}", 
                                currentIndex + 1, totalLines, topicName);
                    }
                } catch (Exception e) {
                    logger.error("发送数据到Kafka时出错", e);
                }
            }, 0, intervalMs, TimeUnit.MILLISECONDS);
            
            return String.format("开始流式发送数据到主题 %s，间隔: %d 毫秒, 数据量: %d 行", 
                    topicName, intervalMs, totalLines);
        } catch (IOException e) {
            logger.error("读取CSV文件失败: {}", filePath, e);
            return "读取文件失败: " + e.getMessage();
        } catch (Exception e) {
            logger.error("流式发送数据失败", e);
            return "发送数据失败: " + e.getMessage();
        }
    }
    
    /**
     * 停止流式发送数据任务
     */
    public void stopStreamTask() {
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
            logger.info("已停止数据流式发送任务");
        }
    }
    
    /**
     * 批量推送CSV文件内容到Kafka
     * 一次性将所有数据发送到Kafka
     * 
     * @param filePath CSV文件路径
     * @param topicName Kafka主题名
     * @return 发送的记录数
     */
    public int batchPushCsvToKafka(String filePath, String topicName) {
        int recordCount = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // 读取并发送标题行
            String header = reader.readLine();
            if (header != null) {
                kafkaClient.sendMessage(topicName, "header", header);
            }
            
            // 读取并发送每一行数据
            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                kafkaClient.sendMessage(topicName, String.valueOf(index), line);
                recordCount++;
                index++;
                
                if (index % 1000 == 0) {
                    logger.info("已发送 {} 行数据到Kafka主题: {}", index, topicName);
                }
            }
            
            logger.info("完成批量发送，共 {} 行数据到Kafka主题: {}", recordCount, topicName);
        } catch (IOException e) {
            logger.error("读取或发送CSV数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法处理CSV数据: " + e.getMessage(), e);
        }
        
        return recordCount;
    }
} 