package org.shiguang.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.shiguang.model.SensorData;
import org.shiguang.websocket.WebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KafkaListenerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaListenerService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private SensorDataService sensorDataService;
    
    @Autowired
    private AnomalyDetectionService anomalyDetectionService;
    
    @Autowired
    private WebSocketHandler webSocketHandler;
    
    /**
     * 监听Kafka消息并处理
     * @param message 从Kafka接收的消息
     */
    @KafkaListener(topics = "${app.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String message) {
        try {
            logger.info("接收到Kafka消息: {}", message);
            
            // 解析JSON消息
            JsonNode json = objectMapper.readTree(message);
            
            // 创建SensorData对象
            SensorData sensorData = new SensorData();
            sensorData.setSensorId(json.get("sensorId").asText());
            sensorData.setTimestamp(json.get("timestamp").asLong());
            sensorData.setTemperature(json.get("temperature").asDouble());
            sensorData.setHumidity(json.get("humidity").asDouble());
            sensorData.setSoilMoisture(json.get("soilMoisture").asDouble());
            sensorData.setLightIntensity(json.get("lightIntensity").asDouble());
            sensorData.setLocation(json.get("location").asText());
            sensorData.setBatteryLevel(json.get("batteryLevel").asDouble());
            
            // 检测异常
            List<SensorData.Anomaly> anomalies = anomalyDetectionService.detectAnomalies(sensorData);
            
            // 广播传感器数据到WebSocket
            webSocketHandler.broadcastSensorData(sensorData);
            
            // 如果有异常，发送异常警报
            if (!anomalies.isEmpty()) {
                webSocketHandler.broadcastAnomalies(sensorData.getSensorId(), sensorData.getTimestamp(), anomalies);
                logger.info("检测到异常, 已发送警报");
            }
            
            // 保存数据到数据库
            sensorDataService.save(sensorData);
            
        } catch (Exception e) {
            logger.error("处理Kafka消息时出错", e);
        }
    }
} 