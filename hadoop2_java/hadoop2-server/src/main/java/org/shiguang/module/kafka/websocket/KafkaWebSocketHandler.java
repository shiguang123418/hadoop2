package org.shiguang.module.kafka.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Kafka WebSocket处理器
 * 实现实时消息推送功能
 */
@Component
public class KafkaWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(KafkaWebSocketHandler.class);
    
    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /**
     * 处理连接建立
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket连接建立: {}", session.getId());
        sessions.put(session.getId(), new SessionInfo(session));
    }
    
    /**
     * 处理接收到的消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        SessionInfo sessionInfo = sessions.get(session.getId());
        
        if (sessionInfo != null) {
            try {
                // 解析请求
                @SuppressWarnings("unchecked")
                Map<String, Object> request = objectMapper.readValue(payload, Map.class);
                String action = (String) request.get("action");
                
                if ("subscribe".equals(action)) {
                    // 订阅主题
                    handleSubscribe(sessionInfo, request);
                } else if ("unsubscribe".equals(action)) {
                    // 取消订阅
                    handleUnsubscribe(sessionInfo, request);
                } else {
                    sendErrorMessage(session, "未知操作: " + action);
                }
            } catch (Exception e) {
                logger.error("处理WebSocket消息失败: {}", e.getMessage(), e);
                sendErrorMessage(session, "处理请求失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 处理连接关闭
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("WebSocket连接关闭: {}, 状态: {}", session.getId(), status);
        
        // 清理资源
        SessionInfo sessionInfo = sessions.remove(session.getId());
        if (sessionInfo != null) {
            sessionInfo.cleanup();
        }
    }
    
    /**
     * 处理传输错误
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket传输错误: {}", exception.getMessage(), exception);
        
        // 关闭连接
        session.close(CloseStatus.SERVER_ERROR);
        
        // 清理资源
        SessionInfo sessionInfo = sessions.remove(session.getId());
        if (sessionInfo != null) {
            sessionInfo.cleanup();
        }
    }
    
    /**
     * 处理订阅请求
     */
    private void handleSubscribe(SessionInfo sessionInfo, Map<String, Object> request) {
        String topic = (String) request.get("topic");
        String groupId = (String) request.getOrDefault("groupId", "ws-consumer-" + UUID.randomUUID());
        
        if (topic == null || topic.isEmpty()) {
            sendErrorMessage(sessionInfo.getSession(), "主题名称不能为空");
            return;
        }
        
        // 如果已经订阅该主题，先取消订阅
        sessionInfo.unsubscribe(topic);
        
        // 创建新的订阅
        KafkaConsumerRunner consumerRunner = new KafkaConsumerRunner(
                bootstrapServers, topic, groupId, sessionInfo);
        
        // 保存订阅信息
        sessionInfo.addSubscription(topic, consumerRunner);
        
        // 启动消费者线程
        executorService.submit(consumerRunner);
        
        // 发送订阅确认消息
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "subscribed");
            response.put("topic", topic);
            response.put("groupId", groupId);
            
            sessionInfo.getSession().sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            logger.error("发送订阅确认消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理取消订阅请求
     */
    private void handleUnsubscribe(SessionInfo sessionInfo, Map<String, Object> request) {
        String topic = (String) request.get("topic");
        
        if (topic == null || topic.isEmpty()) {
            sendErrorMessage(sessionInfo.getSession(), "主题名称不能为空");
            return;
        }
        
        // 取消订阅
        sessionInfo.unsubscribe(topic);
        
        // 发送取消订阅确认消息
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "unsubscribed");
            response.put("topic", topic);
            
            sessionInfo.getSession().sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            logger.error("发送取消订阅确认消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "error");
            response.put("message", errorMessage);
            
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            logger.error("发送错误消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 会话信息类
     * 保存WebSocket会话和订阅信息
     */
    private static class SessionInfo {
        private final WebSocketSession session;
        private final Map<String, KafkaConsumerRunner> subscriptions = new ConcurrentHashMap<>();
        
        public SessionInfo(WebSocketSession session) {
            this.session = session;
        }
        
        public WebSocketSession getSession() {
            return session;
        }
        
        public void addSubscription(String topic, KafkaConsumerRunner consumerRunner) {
            subscriptions.put(topic, consumerRunner);
        }
        
        public void unsubscribe(String topic) {
            KafkaConsumerRunner consumerRunner = subscriptions.remove(topic);
            if (consumerRunner != null) {
                consumerRunner.shutdown();
            }
        }
        
        public void cleanup() {
            // 关闭所有订阅
            for (KafkaConsumerRunner runner : subscriptions.values()) {
                runner.shutdown();
            }
            subscriptions.clear();
        }
    }
    
    /**
     * Kafka消费者运行器
     * 在单独的线程中运行Kafka消费者
     */
    private class KafkaConsumerRunner implements Runnable {
        private final String bootstrapServers;
        private final String topic;
        private final String groupId;
        private final SessionInfo sessionInfo;
        private final KafkaConsumer<String, String> consumer;
        private volatile boolean running = true;
        
        public KafkaConsumerRunner(String bootstrapServers, String topic, String groupId, SessionInfo sessionInfo) {
            this.bootstrapServers = bootstrapServers;
            this.topic = topic;
            this.groupId = groupId;
            this.sessionInfo = sessionInfo;
            
            // 创建Kafka消费者
            Properties props = new Properties();
            props.put("bootstrap.servers", bootstrapServers);
            props.put("group.id", groupId);
            props.put("key.deserializer", StringDeserializer.class.getName());
            props.put("value.deserializer", StringDeserializer.class.getName());
            props.put("auto.offset.reset", "latest");
            props.put("enable.auto.commit", "true");
            
            this.consumer = new KafkaConsumer<>(props);
        }
        
        @Override
        public void run() {
            try {
                // 订阅主题
                consumer.subscribe(Collections.singletonList(topic));
                
                // 消费消息
                while (running) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        if (!running) {
                            break;
                        }
                        
                        try {
                            // 构造消息
                            Map<String, Object> message = new HashMap<>();
                            message.put("type", "message");
                            message.put("topic", record.topic());
                            message.put("partition", record.partition());
                            message.put("offset", record.offset());
                            message.put("key", record.key());
                            message.put("value", record.value());
                            message.put("timestamp", record.timestamp());
                            
                            // 发送消息
                            sessionInfo.getSession().sendMessage(
                                    new TextMessage(objectMapper.writeValueAsString(message)));
                        } catch (IOException e) {
                            logger.error("发送消息失败: {}", e.getMessage(), e);
                            if (!sessionInfo.getSession().isOpen()) {
                                shutdown();
                                break;
                            }
                        }
                    }
                }
            } catch (WakeupException e) {
                // 正常关闭
                if (running) {
                    logger.error("Kafka消费者被意外唤醒: {}", e.getMessage(), e);
                }
            } catch (Exception e) {
                logger.error("Kafka消费者出错: {}", e.getMessage(), e);
            } finally {
                consumer.close();
                logger.info("Kafka消费者已关闭, 主题: {}, 分组: {}", topic, groupId);
            }
        }
        
        /**
         * 关闭消费者
         */
        public void shutdown() {
            running = false;
            consumer.wakeup();
        }
    }
} 