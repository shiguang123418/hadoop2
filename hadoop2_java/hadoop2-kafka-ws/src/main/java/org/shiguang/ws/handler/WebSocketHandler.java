package org.shiguang.ws.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket消息处理程序
 * 用于处理客户端连接和消息发送
 */
@Controller
public class WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    
    // 存储会话ID和用户信息的映射
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    
    /**
     * 处理客户端发送的消息，并广播到所有订阅者
     * @param message 客户端发送的消息
     * @return 广播给所有订阅者的消息
     */
    @MessageMapping("/send")
    @SendTo("/topic/public")
    public String sendMessage(String message) {
        logger.info("收到消息: {}", message);
        return message;
    }
    
    /**
     * 监听客户端连接事件
     * @param event 连接事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        logger.info("客户端已连接: {}", sessionId);
        sessionUserMap.put(sessionId, "Anonymous");
    }
    
    /**
     * 监听客户端断开连接事件
     * @param event 断开连接事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        if(sessionId != null) {
            logger.info("客户端已断开连接: {}", sessionId);
            sessionUserMap.remove(sessionId);
        }
    }
} 