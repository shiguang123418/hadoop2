package com.example.websocketdemo.handler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    
    @Value("${websocket.enabled:true}")
    private boolean websocketEnabled;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("尝试建立WebSocket连接: {}", session.getId());
        log.info("远程地址: {}", session.getRemoteAddress());
        log.info("本地地址: {}", session.getLocalAddress());
        log.info("握手信息: {}", session.getHandshakeHeaders());
        
        if (!websocketEnabled) {
            log.warn("WebSocket连接被禁用。正在关闭连接: {}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("WebSocket service is currently disabled"));
            return;
        }
        
        sessions.add(session);
        log.info("新的WebSocket连接已建立: {}", session.getId());
        session.sendMessage(new TextMessage("Connected to server successfully!"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (!websocketEnabled) {
            log.warn("WebSocket服务被禁用。消息未处理，来自会话: {}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("WebSocket service is currently disabled"));
            return;
        }
        
        String payload = message.getPayload();
        log.info("收到消息: {} 来自会话: {}", payload, session.getId());
        
        // Echo the message back with a prefix
        String response = "Server received: " + payload;
        session.sendMessage(new TextMessage(response));
        
        // Broadcast to all other sessions
        broadcastMessage(session, payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("WebSocket连接关闭: {}, 状态: {}", session.getId(), status);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误，会话ID: {}", session.getId(), exception);
        sessions.remove(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR.withReason("传输错误: " + exception.getMessage()));
        }
    }

    private void broadcastMessage(WebSocketSession senderSession, String message) {
        if (!websocketEnabled) {
            return;
        }
        
        String broadcastMessage = "Broadcast from " + senderSession.getId() + ": " + message;
        sessions.stream()
                .filter(session -> !session.getId().equals(senderSession.getId()))
                .forEach(session -> {
                    try {
                        session.sendMessage(new TextMessage(broadcastMessage));
                    } catch (IOException e) {
                        log.error("向会话发送消息时出错 {}", session.getId(), e);
                    }
                });
    }
}