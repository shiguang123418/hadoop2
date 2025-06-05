package org.shiguang.module.spark.controller;

import org.shiguang.module.common.controller.BaseController;
import org.shiguang.module.common.response.ApiResponse;
import org.shiguang.module.spark.service.SparkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spark控制器
 * 提供Spark相关REST接口
 */
@RestController
@RequestMapping("/spark")
@CrossOrigin
public class SparkController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(SparkController.class);

//    @Autowired
//    private SparkService sparkService;

    @Value("${spark.master}")
    private String sparkMaster;

    /**
     * 获取Spark状态
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSparkStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            // 这里应该实现实际的Spark连接检查
            // 暂时模拟连接成功
            boolean connected = true;
            status.put("connected", connected);
            status.put("url", sparkMaster);
            return ResponseEntity.ok(new ApiResponse<>(200, "Spark状态获取成功", status));
        } catch (Exception e) {
            logger.error("获取Spark状态失败", e);
            status.put("connected", false);
            status.put("url", sparkMaster);
            status.put("error", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(200, "Spark状态获取成功", status));
        }
    }
    
    /**
     * 获取Spark应用列表
     */
    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getApplications() {
        try {
            // 模拟返回一些示例应用
            List<Map<String, Object>> applications = new ArrayList<>();
            
            Map<String, Object> app1 = new HashMap<>();
            app1.put("id", "app-20210615123456-0001");
            app1.put("name", "Spark SQL");
            app1.put("status", "RUNNING");
            app1.put("startTime", System.currentTimeMillis() - 3600000);
            applications.add(app1);
            
            Map<String, Object> app2 = new HashMap<>();
            app2.put("id", "app-20210615123456-0002");
            app2.put("name", "Streaming Job");
            app2.put("status", "RUNNING");
            app2.put("startTime", System.currentTimeMillis() - 7200000);
            applications.add(app2);
            
            return ResponseEntity.ok(new ApiResponse<>(200, "获取应用列表成功", applications));
        } catch (Exception e) {
            logger.error("获取应用列表失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "获取应用列表失败: " + e.getMessage(), null));
        }
    }

}