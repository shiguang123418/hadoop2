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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据推送工具类，用于模拟实时数据
 */
@Component
public class DataPushUtil {
    private static final Logger logger = LoggerFactory.getLogger(DataPushUtil.class);
    
    @Autowired
    private KafkaClient kafkaClient;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    /**
     * 以流模式按行推送CSV文件内容到Kafka
     * 
     * @param filePath 文件路径
     * @param topicName Kafka主题
     * @param intervalMs 每行数据推送间隔(毫秒)
     * @param repeat 是否重复推送
     * @return 任务ID，用于停止推送
     */
    public String streamCsvToKafka(String filePath, String topicName, long intervalMs, boolean repeat) {
        String taskId = "task-" + System.currentTimeMillis();
        
        try {
            // 读取文件内容
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            if (lines.isEmpty()) {
                logger.warn("CSV文件 {} 为空", filePath);
                return null;
            }
            
            final String header = lines.get(0);
            final List<String> dataLines = lines.subList(1, lines.size());
            final AtomicInteger lineIndex = new AtomicInteger(0);
            
            // 先发送表头
            kafkaClient.sendMessage(topicName, "header", header);
            logger.info("已发送CSV表头到主题 {}", topicName);
            
            // 定时发送数据行
            Runnable task = () -> {
                int index = lineIndex.getAndIncrement();
                
                // 如果到文件末尾，根据参数决定是否重新开始
                if (index >= dataLines.size()) {
                    if (repeat) {
                        lineIndex.set(0);
                        index = 0;
                    } else {
                        logger.info("已完成CSV文件 {} 到主题 {} 的数据推送", filePath, topicName);
                        scheduler.shutdown();
                        return;
                    }
                }
                
                String line = dataLines.get(index);
                kafkaClient.sendMessage(topicName, String.valueOf(index), line);
                
                if (index % 10 == 0) { // 每10条记录记录一次日志
                    logger.info("已推送 {} 行数据到主题 {}", index + 1, topicName);
                }
            };
            
            // 启动定时任务
            scheduler.scheduleAtFixedRate(task, 0, intervalMs, TimeUnit.MILLISECONDS);
            logger.info("已启动CSV数据流推送任务: 文件={}, 主题={}, 间隔={}ms", filePath, topicName, intervalMs);
            
            return taskId;
            
        } catch (IOException e) {
            logger.error("读取CSV文件失败: {}", filePath, e);
            return null;
        }
    }
    
    /**
     * 停止所有数据推送任务
     */
    public void stopAllTasks() {
        scheduler.shutdownNow();
        logger.info("已停止所有数据推送任务");
    }
    
    /**
     * 批量推送CSV文件，每行对应一条消息
     * 
     * @param filePath CSV文件路径
     * @param topicName Kafka主题名
     * @return 推送的记录数
     */
    public int batchPushCsvToKafka(String filePath, String topicName) {
        AtomicInteger count = new AtomicInteger(0);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String header = reader.readLine(); // 第一行是表头
            if (header != null) {
                kafkaClient.sendMessage(topicName, "header", header);
                
                // 读取并发送每一行数据
                String line;
                while ((line = reader.readLine()) != null) {
                    int index = count.incrementAndGet();
                    kafkaClient.sendMessage(topicName, String.valueOf(index), line);
                    
                    if (index % 100 == 0) {
                        logger.info("已推送 {} 行数据到主题 {}", index, topicName);
                    }
                }
            }
            
            logger.info("完成批量推送，共 {} 行数据推送到主题 {}", count.get(), topicName);
            return count.get();
            
        } catch (IOException e) {
            logger.error("批量推送数据失败", e);
            return count.get();
        }
    }
} 