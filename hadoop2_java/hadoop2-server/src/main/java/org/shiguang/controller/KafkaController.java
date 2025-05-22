package org.shiguang.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kafka操作的REST API控制器
 */
@RestController
@RequestMapping("/kafka")
@CrossOrigin
public class KafkaController {
    private static final Logger logger = LoggerFactory.getLogger(KafkaController.class);
    
    @Value("${kafka.host}")
    private String kafkaHost;

    @Value("${kafka.port}")
    private int kafkaPort;
    
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
    
    // 主题信息类
    public static class KafkaTopic {
        private String name;
        private int partitions;
        private int replicationFactor;
        private boolean internal;
        private Map<String, String> configs;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public int getPartitions() {
            return partitions;
        }
        
        public void setPartitions(int partitions) {
            this.partitions = partitions;
        }
        
        public int getReplicationFactor() {
            return replicationFactor;
        }
        
        public void setReplicationFactor(int replicationFactor) {
            this.replicationFactor = replicationFactor;
        }
        
        public boolean isInternal() {
            return internal;
        }
        
        public void setInternal(boolean internal) {
            this.internal = internal;
        }
        
        public Map<String, String> getConfigs() {
            return configs;
        }
        
        public void setConfigs(Map<String, String> configs) {
            this.configs = configs;
        }
    }

    /**
     * 检查Kafka服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Object> getStatus() {
        logger.info("检查Kafka连接状态");
        try {
            // 通过尝试建立TCP连接检查Kafka是否可用
            boolean connected = false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(kafkaHost, kafkaPort), 5000); // 5秒连接超时
                connected = socket.isConnected();
            } catch (Exception e) {
                logger.error("连接Kafka失败", e);
                connected = false;
            }
            
            StatusResponse status = new StatusResponse(connected);
            status.setUrl(kafkaHost + ":" + kafkaPort);
            
            logger.info("Kafka连接状态: {}", connected);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("检查Kafka连接状态失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 获取Kafka主题列表
     */
    @GetMapping("/topics")
    public ResponseEntity<?> getTopics() {
        logger.info("获取Kafka主题列表");
        try {
            // 检查Kafka是否可连接
            boolean connected = false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(kafkaHost, kafkaPort), 5000);
                connected = socket.isConnected();
            } catch (Exception e) {
                connected = false;
            }
            
            if (!connected) {
                logger.error("获取主题列表失败: Kafka服务不可用");
                ErrorResponse error = new ErrorResponse("Kafka服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            // 这里只是模拟数据
            // 实际应该使用KafkaAdminClient获取主题列表
            List<KafkaTopic> topics = new ArrayList<>();
            
            KafkaTopic topic1 = new KafkaTopic();
            topic1.setName("clickstream");
            topic1.setPartitions(3);
            topic1.setReplicationFactor(1);
            topic1.setInternal(false);
            Map<String, String> configs1 = new HashMap<>();
            configs1.put("retention.ms", "604800000");
            topic1.setConfigs(configs1);
            topics.add(topic1);
            
            KafkaTopic topic2 = new KafkaTopic();
            topic2.setName("user-events");
            topic2.setPartitions(5);
            topic2.setReplicationFactor(2);
            topic2.setInternal(false);
            Map<String, String> configs2 = new HashMap<>();
            configs2.put("retention.ms", "86400000");
            topic2.setConfigs(configs2);
            topics.add(topic2);
            
            KafkaTopic topic3 = new KafkaTopic();
            topic3.setName("__consumer_offsets");
            topic3.setPartitions(50);
            topic3.setReplicationFactor(1);
            topic3.setInternal(true);
            topics.add(topic3);
            
            logger.info("获取Kafka主题列表成功，主题数量: {}", topics.size());
            return ResponseEntity.ok(topics);
        } catch (Exception e) {
            logger.error("获取Kafka主题列表失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 获取特定主题信息
     */
    @GetMapping("/topics/{topicName}")
    public ResponseEntity<?> getTopicInfo(@PathVariable String topicName) {
        logger.info("获取Kafka主题信息: {}", topicName);
        try {
            // 检查Kafka是否可连接
            boolean connected = false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(kafkaHost, kafkaPort), 5000);
                connected = socket.isConnected();
            } catch (Exception e) {
                connected = false;
            }
            
            if (!connected) {
                logger.error("获取主题信息失败: Kafka服务不可用");
                ErrorResponse error = new ErrorResponse("Kafka服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            // 模拟主题信息
            KafkaTopic topic = null;
            if ("clickstream".equals(topicName)) {
                topic = new KafkaTopic();
                topic.setName("clickstream");
                topic.setPartitions(3);
                topic.setReplicationFactor(1);
                topic.setInternal(false);
                Map<String, String> configs = new HashMap<>();
                configs.put("retention.ms", "604800000");
                topic.setConfigs(configs);
            } else if ("user-events".equals(topicName)) {
                topic = new KafkaTopic();
                topic.setName("user-events");
                topic.setPartitions(5);
                topic.setReplicationFactor(2);
                topic.setInternal(false);
                Map<String, String> configs = new HashMap<>();
                configs.put("retention.ms", "86400000");
                topic.setConfigs(configs);
            } else {
                logger.warn("请求的主题不存在: {}", topicName);
                ErrorResponse error = new ErrorResponse("主题不存在: " + topicName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            logger.info("获取主题信息成功: {}", topicName);
            return ResponseEntity.ok(topic);
        } catch (Exception e) {
            logger.error("获取Kafka主题信息失败: " + topicName, e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 创建主题
     */
    @PostMapping("/topics")
    public ResponseEntity<?> createTopic(@RequestBody KafkaTopic topic) {
        logger.info("创建Kafka主题: {}", topic.getName());
        try {
            // 检查Kafka是否可连接
            boolean connected = false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(kafkaHost, kafkaPort), 5000);
                connected = socket.isConnected();
            } catch (Exception e) {
                connected = false;
            }
            
            if (!connected) {
                logger.error("创建主题失败: Kafka服务不可用");
                ErrorResponse error = new ErrorResponse("Kafka服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            // 这里应该是实际创建主题的代码
            // 目前只是模拟成功
            ApiResponse response = new ApiResponse(true, "主题创建成功: " + topic.getName());
            
            logger.info("创建Kafka主题成功: {}", topic.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("创建Kafka主题失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 删除主题
     */
    @DeleteMapping("/topics/{topicName}")
    public ResponseEntity<?> deleteTopic(@PathVariable String topicName) {
        logger.info("删除Kafka主题: {}", topicName);
        try {
            // 检查Kafka是否可连接
            boolean connected = false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(kafkaHost, kafkaPort), 5000);
                connected = socket.isConnected();
            } catch (Exception e) {
                connected = false;
            }
            
            if (!connected) {
                logger.error("删除主题失败: Kafka服务不可用");
                ErrorResponse error = new ErrorResponse("Kafka服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            // 这里应该是实际删除主题的代码
            // 目前只是模拟成功
            ApiResponse response = new ApiResponse(true, "主题删除成功: " + topicName);
            
            logger.info("删除Kafka主题成功: {}", topicName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除Kafka主题失败: " + topicName, e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 获取集群信息
     */
    @GetMapping("/cluster/info")
    public ResponseEntity<?> getClusterInfo() {
        logger.info("获取Kafka集群信息");
        try {
            // 检查Kafka是否可连接
            boolean connected = false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(kafkaHost, kafkaPort), 5000);
                connected = socket.isConnected();
            } catch (Exception e) {
                connected = false;
            }
            
            if (!connected) {
                logger.error("获取集群信息失败: Kafka服务不可用");
                ErrorResponse error = new ErrorResponse("Kafka服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            // 模拟集群信息
            Map<String, Object> clusterInfo = new HashMap<>();
            clusterInfo.put("clusterId", "pzxd4rQdT2-eMrPJJkIYPQ");
            clusterInfo.put("version", "3.3.1");
            
            List<Map<String, Object>> brokers = new ArrayList<>();
            
            Map<String, Object> broker1 = new HashMap<>();
            broker1.put("id", 1);
            broker1.put("host", "kafka-broker-1");
            broker1.put("port", 9092);
            broker1.put("rack", "rack1");
            brokers.add(broker1);
            
            Map<String, Object> broker2 = new HashMap<>();
            broker2.put("id", 2);
            broker2.put("host", "kafka-broker-2");
            broker2.put("port", 9092);
            broker2.put("rack", "rack2");
            brokers.add(broker2);
            
            Map<String, Object> broker3 = new HashMap<>();
            broker3.put("id", 3);
            broker3.put("host", "kafka-broker-3");
            broker3.put("port", 9092);
            broker3.put("rack", "rack3");
            brokers.add(broker3);
            
            clusterInfo.put("brokers", brokers);
            
            logger.info("获取Kafka集群信息成功");
            return ResponseEntity.ok(clusterInfo);
        } catch (Exception e) {
            logger.error("获取Kafka集群信息失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
} 