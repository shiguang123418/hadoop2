package org.shiguang.module.stock.Controller;

import org.shiguang.module.stock.config.StockConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 股票数据模式控制器
 * 提供简单的接口用于切换实时/模拟模式
 */
@RestController
@RequestMapping("/stock/mode")
public class StockModeController {

    private static final Logger logger = LoggerFactory.getLogger(StockModeController.class);
    
    @Autowired
    private StockConfig stockConfig;
    
    /**
     * 获取当前模式状态
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCurrentMode() {
        Map<String, Object> result = new HashMap<>();
        
        // 主要状态
        result.put("enabled", stockConfig.isEnabled());
        result.put("simulation", stockConfig.isSimulationEnabled());
        
        // 其他参数
        Map<String, String> targetStockMap = new HashMap<>();
        targetStockMap.put("code", stockConfig.getSimulationTargetCode());
        targetStockMap.put("name", stockConfig.getSimulationTargetName());
        result.put("targetStock", targetStockMap);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 启用/禁用股票模块
     */
    @PostMapping("/enabled")
    public ResponseEntity<Map<String, Object>> setEnabled(
            @RequestParam(required = true) boolean enabled) {
        stockConfig.setEnabled(enabled);
        logger.info("股票模块已{}", enabled ? "启用" : "禁用");
        return createSimpleResponse("股票模块已" + (enabled ? "启用" : "禁用"), "enabled", enabled);
    }
    
    /**
     * 切换为实时模式
     */
    @PostMapping("/realtime")
    public ResponseEntity<Map<String, Object>> switchToRealtime() {
        stockConfig.setSimulationEnabled(false);
        logger.info("已切换为实时模式");
        return createResponse("已切换为实时模式", false);
    }
    
    /**
     * 切换为模拟模式
     */
    @PostMapping("/simulation")
    public ResponseEntity<Map<String, Object>> switchToSimulation() {
        stockConfig.setSimulationEnabled(true);
        logger.info("已切换为模拟模式");
        return createResponse("已切换为模拟模式", true);
    }
    
    /**
     * 设置目标股票
     */
    @PostMapping("/target")
    public ResponseEntity<Map<String, Object>> setTargetStock(
            @RequestParam(defaultValue = "002714") String code, 
            @RequestParam(defaultValue = "牧原股份") String name) {
        stockConfig.setSimulationTargetCode(code);
        stockConfig.setSimulationTargetName(name);
        stockConfig.setDefaultStockCode(code); // 同时更新爬取的默认股票代码
        logger.info("已设置目标股票: {} ({})", name, code);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", String.format("已设置目标股票: %s (%s)", name, code));
        
        Map<String, String> targetStockMap = new HashMap<>();
        targetStockMap.put("code", code);
        targetStockMap.put("name", name);
        result.put("targetStock", targetStockMap);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 创建响应
     */
    private ResponseEntity<Map<String, Object>> createResponse(String message, boolean isSimulation) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", message);
        result.put("mode", isSimulation ? "simulation" : "realtime");
        return ResponseEntity.ok(result);
    }

    /**
     * 创建简单响应
     */
    private ResponseEntity<Map<String, Object>> createSimpleResponse(String message, String key, Object value) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", message);
        result.put(key, value);
        return ResponseEntity.ok(result);
    }
} 