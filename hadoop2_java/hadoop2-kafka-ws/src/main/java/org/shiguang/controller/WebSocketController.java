package org.shiguang.controller;

import org.shiguang.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/websocket")
public class WebSocketController {

    @Autowired
    private AppProperties appProperties;

    /**
     * 提供一个测试页面，用于查看WebSocket连接
     */
    @GetMapping("/test")
    public String websocketTest(Model model) {
        String wsProtocol = "ws";
        String serverHost = "localhost";
        int serverPort = appProperties.getWebsocket().getPort();
        String wsPath = appProperties.getWebsocket().getPath();
        
        String wsUrl = wsProtocol + "://" + serverHost + ":" + serverPort + wsPath;
        model.addAttribute("wsUrl", wsUrl);
        
        return "websocket-test";
    }
} 