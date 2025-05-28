package org.shiguang.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket配置类
 * 用于处理WebSocket连接和STOMP消息
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单的基于内存的消息代理，将消息传递回客户端
        config.enableSimpleBroker("/topic", "/queue");
        // 配置一个或多个前缀以过滤针对带注释的方法的消息
        config.setApplicationDestinationPrefixes("/app");
        // 设置用户目标前缀
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点，客户端将使用这些端点与服务器建立WebSocket连接
        registry.addEndpoint("/api/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
    
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // 配置传输选项
        registration.setMessageSizeLimit(8192 * 4) // 默认值是64 * 1024 bytes
                    .setSendBufferSizeLimit(512 * 1024) // 默认值是512 * 1024 bytes
                    .setSendTimeLimit(30 * 1000); // 默认值是10秒
    }
} 