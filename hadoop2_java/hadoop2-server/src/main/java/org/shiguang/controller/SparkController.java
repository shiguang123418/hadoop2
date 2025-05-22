package org.shiguang.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spark操作的REST API控制器
 */
@RestController
@RequestMapping("/spark")
@CrossOrigin
public class SparkController {
    private static final Logger logger = LoggerFactory.getLogger(SparkController.class);
    
    @Value("${spark.master.url}")
    private String sparkMasterUrl;
    
    // API响应基类
    public static class ApiResponse {
        private boolean success;
        private String message;

        public ApiResponse() {
        }

        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    // 错误响应类
    public static class ErrorResponse extends ApiResponse {
        private String error;

        public ErrorResponse() {
            setSuccess(false);
        }

        public ErrorResponse(String error) {
            setSuccess(false);
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    // 状态响应类
    public static class StatusResponse extends ApiResponse {
        private boolean connected;
        private String url;

        public StatusResponse(boolean connected) {
            super(true, connected ? "已连接" : "未连接");
            this.connected = connected;
        }

        public boolean isConnected() {
            return connected;
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
    
    // 应用程序信息类
    public static class SparkApplication {
        private String id;
        private String name;
        private String state;
        private String startTime;
        private String endTime;
        private int cores;
        private String user;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getState() {
            return state;
        }
        
        public void setState(String state) {
            this.state = state;
        }
        
        public String getStartTime() {
            return startTime;
        }
        
        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }
        
        public String getEndTime() {
            return endTime;
        }
        
        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
        
        public int getCores() {
            return cores;
        }
        
        public void setCores(int cores) {
            this.cores = cores;
        }
        
        public String getUser() {
            return user;
        }
        
        public void setUser(String user) {
            this.user = user;
        }
    }

    /**
     * 检查Spark服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Object> getStatus() {
        logger.info("检查Spark连接状态");
        try {
            URL url = new URL(sparkMasterUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000); // 设置连接超时为5秒
            connection.setRequestMethod("GET");
            
            int responseCode = connection.getResponseCode();
            boolean connected = (responseCode == 200);
            
            StatusResponse status = new StatusResponse(connected);
            status.setUrl(sparkMasterUrl);
            
            logger.info("Spark连接状态: {}, 响应码: {}", connected, responseCode);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("检查Spark连接状态失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 获取应用列表
     */
    @GetMapping("/applications")
    public ResponseEntity<?> getApplications() {
        logger.info("获取Spark应用列表");
        try {
            // 判断Spark是否可连接
            URL url = new URL(sparkMasterUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("GET");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                logger.error("获取Spark应用列表失败: Spark服务不可用");
                ErrorResponse error = new ErrorResponse("Spark服务不可用，HTTP状态码: " + responseCode);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            // 这里只是模拟数据
            // 实际应该使用Spark REST API获取应用列表
            List<SparkApplication> applications = new ArrayList<>();
            
            SparkApplication app1 = new SparkApplication();
            app1.setId("app-20231015123456-0001");
            app1.setName("数据清洗任务");
            app1.setState("RUNNING");
            app1.setStartTime("2023-10-15 12:34:56");
            app1.setCores(4);
            app1.setUser("hadoop");
            applications.add(app1);
            
            SparkApplication app2 = new SparkApplication();
            app2.setId("app-20231015130145-0002");
            app2.setName("批处理数据分析");
            app2.setState("FINISHED");
            app2.setStartTime("2023-10-15 13:01:45");
            app2.setEndTime("2023-10-15 13:15:23");
            app2.setCores(8);
            app2.setUser("spark");
            applications.add(app2);
            
            logger.info("获取Spark应用列表成功，应用数量: {}", applications.size());
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            logger.error("获取Spark应用列表失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 获取环境信息
     */
    @GetMapping("/environment")
    public ResponseEntity<?> getEnvironment() {
        logger.info("获取Spark环境信息");
        try {
            // 判断Spark是否可连接
            boolean connected = false;
            try {
                URL url = new URL(sparkMasterUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setRequestMethod("GET");
                connected = (connection.getResponseCode() == 200);
            } catch (Exception e) {
                logger.error("检查Spark连接状态失败", e);
                connected = false;
            }
            
            if (!connected) {
                ErrorResponse error = new ErrorResponse("Spark服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            // 这里只是模拟数据
            // 实际应该获取真实的Spark环境信息
            Map<String, Object> environment = new HashMap<>();
            environment.put("sparkVersion", "3.3.2");
            environment.put("javaVersion", "1.8.0_352");
            environment.put("scalaVersion", "2.12.15");
            environment.put("master", "spark://master:7077");
            
            Map<String, Object> runtime = new HashMap<>();
            runtime.put("jvmName", "OpenJDK 64-Bit Server VM");
            runtime.put("javaHome", "/usr/lib/jvm/java-8-openjdk-amd64");
            environment.put("runtime", runtime);
            
            List<Map<String, String>> workers = new ArrayList<>();
            Map<String, String> worker1 = new HashMap<>();
            worker1.put("id", "worker-1");
            worker1.put("host", "worker1");
            worker1.put("port", "7078");
            worker1.put("cores", "8");
            worker1.put("memory", "16g");
            workers.add(worker1);
            
            Map<String, String> worker2 = new HashMap<>();
            worker2.put("id", "worker-2");
            worker2.put("host", "worker2");
            worker2.put("port", "7078");
            worker2.put("cores", "8");
            worker2.put("memory", "16g");
            workers.add(worker2);
            
            environment.put("workers", workers);
            
            logger.info("获取Spark环境信息成功");
            return ResponseEntity.ok(environment);
        } catch (Exception e) {
            logger.error("获取Spark环境信息失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
} 