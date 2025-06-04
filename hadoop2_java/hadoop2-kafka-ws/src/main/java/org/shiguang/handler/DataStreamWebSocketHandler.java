package org.shiguang.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

/**
 * 数据流WebSocket处理器
 * 负责将Spark处理的数据转发到前端
 */
@Component
public class DataStreamWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(DataStreamWebSocketHandler.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 存储客户端订阅信息
    private final Map<String, Integer> topicSubscriberCount = new ConcurrentHashMap<>();
    
    /**
     * 发送天气数据统计结果到WebSocket客户端
     * 
     * @param data 天气数据统计结果
     */
    public void sendWeatherStats(List<Map<String, Object>> data) {
        try {
            // 将数据发送到天气统计主题
            messagingTemplate.convertAndSend("/topic/weather-stats", data);
            logger.info("已发送{}条天气统计数据到WebSocket客户端", data.size());
        } catch (Exception e) {
            logger.error("发送天气统计数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送产品数据统计结果到WebSocket客户端
     * 
     * @param data 产品数据统计结果
     */
    public void sendProductStats(List<Map<String, Object>> data) {
        try {
            // 将数据发送到产品统计主题
            messagingTemplate.convertAndSend("/topic/product-stats", data);
            logger.info("已发送{}条产品统计数据到WebSocket客户端", data.size());
        } catch (Exception e) {
            logger.error("发送产品统计数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送JSON字符串列表到指定主题
     * 
     * @param destination 目标主题
     * @param jsonResults JSON字符串列表
     */
    public void sendJsonStringList(String destination, List<String> jsonResults) {
        try {
            messagingTemplate.convertAndSend(destination, jsonResults);
            logger.info("已发送{}条JSON数据到主题: {}", jsonResults.size(), destination);
        } catch (Exception e) {
            logger.error("发送数据到主题{}失败: {}", destination, e.getMessage(), e);
        }
    }
    
    /**
     * 发送通用更新消息
     * 
     * @param message 消息内容
     */
    public void sendUpdateMessage(String message) {
        try {
            messagingTemplate.convertAndSend("/topic/updates", message);
            logger.info("已发送更新消息: {}", message);
        } catch (Exception e) {
            logger.error("发送更新消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送系统通知
     * 
     * @param type 通知类型 (info, warning, error)
     * @param title 通知标题
     * @param message 通知内容
     */
    public void sendNotification(String type, String title, String message) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("title", title);
            notification.put("message", message);
            notification.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/notifications", notification);
            logger.info("已发送{}类型通知: {}", type, title);
        } catch (Exception e) {
            logger.error("发送通知失败: {}", e.getMessage(), e);
        }
    }
} 