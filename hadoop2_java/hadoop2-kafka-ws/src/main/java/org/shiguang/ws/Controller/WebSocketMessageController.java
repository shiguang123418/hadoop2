package org.shiguang.ws.Controller;

import org.shiguang.handler.DataStreamWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket消息控制器
 * 处理WebSocket客户端发送的STOMP消息
 */
@Controller
public class WebSocketMessageController {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageController.class);
    
    @Autowired
    private DataStreamWebSocketHandler dataStreamWebSocketHandler;
    


    /**
     * 处理客户端订阅请求
     * 
     * @param request 订阅请求
     * @return 订阅确认
     */
    @MessageMapping("/subscribe")
    @SendTo("/topic/subscriptions")
    public Map<String, Object> handleSubscription(Map<String, String> request) {
        String topic = request.get("topic");
        String clientId = request.get("clientId");
        
        logger.info("客户端 {} 订阅主题: {}", clientId, topic);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "已成功订阅主题: " + topic);
        response.put("topic", topic);
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }
    
    /**
     * 处理客户端发送的数据请求
     * 
     * @param request 数据请求
     */
    @MessageMapping("/request-data")
    public void handleDataRequest(Map<String, String> request) {
        String dataType = request.get("dataType");
        String clientId = request.get("clientId");
        
        logger.info("客户端 {} 请求 {} 类型数据", clientId, dataType);
        
        // 发送通知消息
        dataStreamWebSocketHandler.sendNotification(
            "info", 
            "数据请求已接收", 
            "正在处理 " + dataType + " 数据请求"
        );
    }
} 