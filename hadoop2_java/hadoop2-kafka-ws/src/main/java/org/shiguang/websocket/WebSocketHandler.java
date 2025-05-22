package org.shiguang.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shiguang.model.SensorData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        logger.info("新WebSocket连接建立: {}, 当前连接数: {}", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        logger.info("WebSocket连接关闭: {}, 原因: {}, 当前连接数: {}", 
                   session.getId(), status.getReason(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 处理客户端发送的消息
        logger.info("收到消息: {} 来自: {}", message.getPayload(), session.getId());
    }

    /**
     * 广播传感器数据到所有连接的客户端
     */
    public void broadcastSensorData(SensorData sensorData) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(sensorData);
            broadcast(jsonMessage);
        } catch (Exception e) {
            logger.error("发送传感器数据时出错", e);
        }
    }

    /**
     * 广播异常数据到所有连接的客户端
     */
    public void broadcastAnomalies(String sensorId, long timestamp, List<SensorData.Anomaly> anomalies) {
        try {
            Map<String, Object> anomalyData = new HashMap<>();
            anomalyData.put("type", "anomaly");
            anomalyData.put("sensorId", sensorId);
            anomalyData.put("timestamp", timestamp);
            anomalyData.put("anomalies", anomalies);
            
            String jsonMessage = objectMapper.writeValueAsString(anomalyData);
            broadcast(jsonMessage);
        } catch (Exception e) {
            logger.error("发送异常数据时出错", e);
        }
    }

    /**
     * 广播消息到所有连接的客户端
     */
    public void broadcast(String message) {
        logger.info("广播消息到 {} 个客户端", sessions.size());
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                logger.error("向客户端 {} 发送消息失败", session.getId(), e);
            }
        });
    }
} 