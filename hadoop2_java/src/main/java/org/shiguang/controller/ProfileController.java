package org.shiguang.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * 配置文件管理控制器
 * 仅用于查看配置信息，不支持运行时切换配置
 */
@RestController
@RequestMapping("/profile")
@Slf4j
public class ProfileController {

    @Autowired
    private Environment environment;

    /**
     * 获取当前激活的配置信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getProfileInfo() {
        Map<String, Object> response = new HashMap<>();
        
        // 获取当前激活的配置文件
        String[] activeProfiles = environment.getActiveProfiles();
        response.put("activeProfiles", activeProfiles);
        
        // 获取默认配置文件
        String[] defaultProfiles = environment.getDefaultProfiles();
        response.put("defaultProfile", defaultProfiles.length > 0 ? defaultProfiles[0] : null);
        
        // 获取应用名称和版本
        response.put("applicationName", environment.getProperty("spring.application.name", "农业大数据平台"));
        response.put("applicationVersion", environment.getProperty("application.version", "1.0.0"));
        
        // 获取服务器端口和上下文路径
        response.put("serverPort", environment.getProperty("server.port"));
        response.put("contextPath", environment.getProperty("server.servlet.context-path"));
        
        log.info("请求配置信息: 活动配置={}", Arrays.toString(activeProfiles));
        
        return ResponseEntity.ok(response);
    }
} 