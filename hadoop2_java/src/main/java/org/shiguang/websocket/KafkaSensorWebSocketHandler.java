package org.shiguang.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shiguang.entity.SensorReading;
import org.shiguang.service.KafkaStreamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Kafka传感器数据WebSocket处理器
 * 负责将Kafka流处理的数据实时推送给前端
 */
@Component
public class KafkaSensorWebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaSensorWebSocketHandler.class);
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    @Autowired
    private KafkaStreamingService kafkaStreamingService;
    
    public KafkaSensorWebSocketHandler() {
        // 启动定期推送数据的任务
        scheduler.scheduleAtFixedRate(this::broadcastStatus, 0, 5, TimeUnit.SECONDS);
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("建立新的WebSocket连接: {}", session.getId());
        sessions.put(session.getId(), session);
        
        // 发送欢迎消息
        sendMessage(session, Map.of(
            "type", "connection",
            "message", "WebSocket连接已建立",
            "connected", true
        ));
        
        // 发送当前状态
        sendStatusToSession(session);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("WebSocket连接关闭: {}, 状态: {}", session.getId(), status);
        sessions.remove(session.getId());
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            logger.debug("接收到WebSocket消息: {}", payload);
            
            // 解析客户端消息
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String type = (String) data.getOrDefault("type", "");
            
            // 根据消息类型处理
            switch (type) {
                case "ping":
                    sendMessage(session, Map.of(
                        "type", "pong",
                        "timestamp", System.currentTimeMillis()
                    ));
                    break;
                case "request_status":
                    sendStatusToSession(session);
                    break;
                default:
                    logger.warn("未知的消息类型: {}", type);
                    break;
            }
        } catch (Exception e) {
            logger.error("处理WebSocket消息时出错", e);
            sendMessage(session, Map.of(
                "type", "error",
                "message", "处理消息失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 向所有连接的客户端广播Kafka状态
     */
    private void broadcastStatus() {
        try {
            if (sessions.isEmpty()) {
                return;
            }
            
            boolean isActive = kafkaStreamingService.isStreamingActive();
            
            // 创建状态消息
            Map<String, Object> statusMessage = Map.of(
                "type", "status",
                "active", isActive,
                "timestamp", System.currentTimeMillis()
            );
            
            // 广播给所有客户端
            broadcast(statusMessage);
        } catch (Exception e) {
            logger.error("广播状态时出错", e);
        }
    }
    
    /**
     * 向指定会话发送Kafka状态
     */
    private void sendStatusToSession(WebSocketSession session) {
        try {
            boolean isActive = kafkaStreamingService.isStreamingActive();
            
            sendMessage(session, Map.of(
                "type", "status",
                "active", isActive,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.error("发送状态时出错", e);
        }
    }
    
    /**
     * 广播传感器数据
     * @param readings 传感器数据列表
     */
    public void broadcastSensorData(List<SensorReading> readings) {
        if (sessions.isEmpty() || readings.isEmpty()) {
            return;
        }
        
        try {
            broadcast(Map.of(
                "type", "sensor_reading",
                "readings", readings,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.error("广播传感器数据时出错", e);
        }
    }
    
    /**
     * 广播单条传感器数据
     * @param reading 传感器数据
     */
    public void broadcastSensorReading(SensorReading reading) {
        if (sessions.isEmpty() || reading == null) {
            return;
        }
        
        try {
            broadcast(Map.of(
                "type", "sensor_reading",
                "reading", reading,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.error("广播传感器数据时出错", e);
        }
    }
    
    /**
     * 广播摘要信息
     * @param summary 摘要数据
     */
    public void broadcastSummary(Map<String, Object> summary) {
        if (sessions.isEmpty() || summary == null) {
            return;
        }
        
        try {
            broadcast(Map.of(
                "type", "summary",
                "summary", summary,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.error("广播摘要数据时出错", e);
        }
    }
    
    /**
     * 广播异常数据
     * @param anomalies 异常数据列表
     */
    public void broadcastAnomalies(List<SensorReading> anomalies) {
        if (sessions.isEmpty() || anomalies.isEmpty()) {
            return;
        }
        
        try {
            broadcast(Map.of(
                "type", "anomaly",
                "anomalies", anomalies,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.error("广播异常数据时出错", e);
        }
    }
    
    /**
     * 向所有连接的客户端广播消息
     * @param message 要广播的消息
     */
    private void broadcast(Map<String, Object> message) {
        try {
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(message));
            
            // 并发安全地迭代会话集合
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        logger.error("发送消息到会话 {} 失败", session.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("序列化消息时出错", e);
        }
    }
    
    /**
     * 向指定会话发送消息
     * @param session 目标会话
     * @param message 要发送的消息
     */
    private void sendMessage(WebSocketSession session, Map<String, Object> message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception e) {
            logger.error("发送消息到会话 {} 失败", session.getId(), e);
        }
    }
} 