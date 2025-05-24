package com.example.websocketdemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.example.websocketdemo.handler.WebSocketHandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

@Configuration
@EnableWebSocket
@ConditionalOnProperty(name = "websocket.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;
    
    @Value("${websocket.endpoint:/ws}")
    private String endpoint;
    
    @Value("${websocket.allowed-origins:*}")
    private String[] allowedOrigins;

    public WebSocketConfig(WebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("注册WebSocket处理程序到路径: {}", endpoint);
        log.info("允许的来源: {}", String.join(", ", allowedOrigins));
        
        if (allowedOrigins.length == 1 && "*".equals(allowedOrigins[0])) {
            // 当使用通配符时，必须使用setAllowedOriginPatterns
            registry.addHandler(webSocketHandler, endpoint)
                    .setAllowedOriginPatterns("*")
                    .withSockJS()
                    .setSupressCors(true); // 禁用SockJS自身的CORS，使用全局CORS配置
        } else {
            // 当使用具体的域名列表时，可以使用setAllowedOrigins
            registry.addHandler(webSocketHandler, endpoint)
                    .setAllowedOrigins(allowedOrigins)
                    .withSockJS()
                    .setSupressCors(true); // 禁用SockJS自身的CORS，使用全局CORS配置
        }
        
        // 添加不使用SockJS的WebSocket处理程序（用于本地开发测试）
        if (allowedOrigins.length == 1 && "*".equals(allowedOrigins[0])) {
            registry.addHandler(webSocketHandler, endpoint)
                    .setAllowedOriginPatterns("*");
        } else {
            registry.addHandler(webSocketHandler, endpoint)
                    .setAllowedOrigins(allowedOrigins);
        }
    }
}