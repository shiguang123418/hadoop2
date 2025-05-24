package org.shiguang.module.common.controller;

import org.shiguang.entity.dto.ApiResponse;
import org.shiguang.module.hdfs.service.HDFSService;
import org.shiguang.module.hdfs.controller.HDFSController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务状态控制器
 * 提供各个服务的连接状态查询API
 */
@RestController
@RequestMapping("/status")
@CrossOrigin
public class ServiceStatusController {

    @Value("${hadoop.hdfs.uri}")
    private String hdfsUri;

    @Value("${hive.url}")
    private String hiveUrl;

    @Value("${spark.master}")
    private String sparkMaster;

    @Value("${kafka.bootstrap.servers}")
    private String kafkaServers;
    
    @Autowired
    private HDFSService hdfsService;
    
    @Autowired
    private HDFSController hdfsController;

    /**
     * 获取HDFS服务状态
     */
    @GetMapping("/hdfs")
    public ApiResponse<Map<String, Object>> getHdfsStatus() {
        try {
            ResponseEntity<ApiResponse<Map<String, Object>>> response = hdfsController.getStatus();
            return response.getBody();
        } catch (Exception e) {
            Map<String, Object> status = new HashMap<>();
            status.put("connected", false);
            status.put("uri", hdfsUri);
            status.put("error", e.getMessage());
            return ApiResponse.success("HDFS状态获取成功", status);
        }
    }

    /**
     * 获取Hive服务状态
     */
    @GetMapping("/hive")
    public ApiResponse<Map<String, Object>> getHiveStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean isConnected = checkHiveConnection();
        status.put("connected", isConnected);
        status.put("url", hiveUrl);
        
        return ApiResponse.success("Hive状态获取成功", status);
    }

    /**
     * 获取Spark服务状态
     */
    @GetMapping("/spark")
    public ApiResponse<Map<String, Object>> getSparkStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean isConnected = checkSparkConnection();
        status.put("connected", isConnected);
        status.put("url", sparkMaster);
        
        return ApiResponse.success("Spark状态获取成功", status);
    }

    /**
     * 获取Kafka服务状态
     */
    @GetMapping("/kafka")
    public ApiResponse<Map<String, Object>> getKafkaStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean isConnected = checkKafkaConnection();
        status.put("connected", isConnected);
        status.put("url", kafkaServers);
        
        return ApiResponse.success("Kafka状态获取成功", status);
    }

    /**
     * 检查HDFS连接状态
     */
    private boolean checkHdfsConnection() {
        try {
            return hdfsService.isHdfsAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查Hive连接状态
     */
    private boolean checkHiveConnection() {
        try {
            // 实际项目中应该使用Hive JDBC连接检查
            // 这里暂时模拟连接成功
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查Spark连接状态
     */
    private boolean checkSparkConnection() {
        try {
            // 实际项目中应该使用Spark客户端检查连接
            // 这里暂时模拟连接成功
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查Kafka连接状态
     */
    private boolean checkKafkaConnection() {
        try {
            // 实际项目中应该使用Kafka客户端检查连接
            // 这里暂时模拟连接成功
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 