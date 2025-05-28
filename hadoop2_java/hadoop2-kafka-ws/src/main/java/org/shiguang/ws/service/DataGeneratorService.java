package org.shiguang.ws.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据生成器服务
 * 用于模拟生成农业传感器数据并发送到Kafka
 */
@Service
public class DataGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(DataGeneratorService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;
    
    @Value("${kafka.topics.agriculture-sensor-data}")
    private String sensorTopic;
    
    @Value("${data.generator.enabled:true}")
    private boolean dataGeneratorEnabled;
    
    @Value("${data.generator.interval:5000}")
    private long dataGenerationInterval;
    
    private ScheduledExecutorService executorService;
    private Producer<String, String> producer;
    private AtomicBoolean running = new AtomicBoolean(false);
    private final Random random = new Random();
    
    // 传感器ID列表，模拟多个传感器
    private final List<String> sensorIds = Arrays.asList(
            "sensor-001", "sensor-002", "sensor-003", "sensor-004", "sensor-005"
    );
    
    // 传感器类型和对应的数据范围
    private final Map<String, Map<String, Double>> sensorTypes = new HashMap<>();
    
    @PostConstruct
    public void init() {
        logger.info("初始化数据生成器，Kafka地址: {}, 主题: {}", bootstrapServers, sensorTopic);
        initSensorTypes();
        if (dataGeneratorEnabled) {
            logger.info("数据生成器已启用，数据生成间隔: {}ms", dataGenerationInterval);
            executorService = Executors.newSingleThreadScheduledExecutor();
            initProducer();
            start();
        } else {
            logger.info("数据生成器已禁用");
        }
    }
    
    @PreDestroy
    public void shutdown() {
        running.set(false);
        if (executorService != null) {
            executorService.shutdown();
        }
        if (producer != null) {
            producer.close();
        }
        logger.info("数据生成器已关闭");
    }
    
    /**
     * 初始化传感器类型及其数据范围
     */
    private void initSensorTypes() {
        // 温度传感器 (摄氏度)
        Map<String, Double> temperatureRange = new HashMap<>();
        temperatureRange.put("min", 15.0);
        temperatureRange.put("max", 35.0);
        sensorTypes.put("temperature", temperatureRange);
        
        // 湿度传感器 (%)
        Map<String, Double> humidityRange = new HashMap<>();
        humidityRange.put("min", 30.0);
        humidityRange.put("max", 90.0);
        sensorTypes.put("humidity", humidityRange);
        
        // 土壤湿度传感器 (%)
        Map<String, Double> soilMoistureRange = new HashMap<>();
        soilMoistureRange.put("min", 20.0);
        soilMoistureRange.put("max", 80.0);
        sensorTypes.put("soilMoisture", soilMoistureRange);
        
        // 光照传感器 (lux)
        Map<String, Double> lightRange = new HashMap<>();
        lightRange.put("min", 0.0);
        lightRange.put("max", 80000.0);
        sensorTypes.put("light", lightRange);
        
        // 二氧化碳传感器 (ppm)
        Map<String, Double> co2Range = new HashMap<>();
        co2Range.put("min", 400.0);
        co2Range.put("max", 2000.0);
        sensorTypes.put("co2", co2Range);
        
        logger.info("已初始化{}种传感器类型", sensorTypes.size());
    }
    
    /**
     * 初始化Kafka生产者
     */
    private void initProducer() {
        try {
            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.ACKS_CONFIG, "1");
            
            producer = new KafkaProducer<>(props);
            logger.info("Kafka生产者已初始化");
        } catch (Exception e) {
            logger.error("初始化Kafka生产者失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 启动数据生成
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            executorService.scheduleAtFixedRate(
                    this::generateAndSendData, 
                    0, 
                    dataGenerationInterval, 
                    TimeUnit.MILLISECONDS
            );
            logger.info("数据生成器已启动，间隔: {}ms", dataGenerationInterval);
        } else {
            logger.info("数据生成器已经在运行中");
        }
    }
    
    /**
     * 生成并发送数据到Kafka
     */
    private void generateAndSendData() {
        try {
            logger.debug("开始生成传感器数据...");
            int sentCount = 0;
            
            for (String sensorId : sensorIds) {
                // 为每个传感器随机选择一种类型
                List<String> types = new ArrayList<>(sensorTypes.keySet());
                String sensorType = types.get(random.nextInt(types.size()));
                
                // 生成在范围内的随机值
                Map<String, Double> range = sensorTypes.get(sensorType);
                double min = range.get("min");
                double max = range.get("max");
                double value = min + (max - min) * random.nextDouble();
                
                // 有5%的概率生成异常值
                if (random.nextDouble() < 0.05) {
                    if (random.nextBoolean()) {
                        value = max + (max * 0.2 * random.nextDouble());
                    } else {
                        value = min - (min * 0.2 * random.nextDouble());
                    }
                }
                
                // 格式化值，保留两位小数
                value = Math.round(value * 100.0) / 100.0;
                
                // 创建传感器数据JSON
                ObjectNode dataNode = objectMapper.createObjectNode();
                dataNode.put("sensorId", sensorId);
                dataNode.put("sensorType", sensorType);
                dataNode.put("value", value);
                dataNode.put("unit", getSensorUnit(sensorType));
                dataNode.put("timestamp", System.currentTimeMillis());
                dataNode.put("location", getRandomLocation());
                dataNode.put("batteryLevel", 50 + random.nextInt(50));
                
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
                dataNode.put("readableTime", dateFormat.format(new Date()));
                
                String jsonData = objectMapper.writeValueAsString(dataNode);
                
                // 发送到Kafka
                producer.send(new ProducerRecord<>(sensorTopic, sensorId, jsonData), (metadata, exception) -> {
                    if (exception == null) {
                        logger.debug("数据已发送到Kafka主题 {}: {}", sensorTopic, jsonData);
                    } else {
                        logger.error("发送数据到Kafka时出错: {}", exception.getMessage(), exception);
                    }
                });
                sentCount++;
            }
            
            logger.info("已生成并发送{}条传感器数据到Kafka", sentCount);
        } catch (Exception e) {
            logger.error("生成或发送数据时出错: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取传感器单位
     */
    private String getSensorUnit(String sensorType) {
        switch (sensorType) {
            case "temperature":
                return "°C";
            case "humidity":
            case "soilMoisture":
                return "%";
            case "light":
                return "lux";
            case "co2":
                return "ppm";
            default:
                return "";
        }
    }
    
    /**
     * 获取随机位置信息
     */
    private String getRandomLocation() {
        String[] locations = {
                "温室A区", "温室B区", "大棚1号", "大棚2号", "田间1区", "田间2区", "仓库"
        };
        return locations[random.nextInt(locations.length)];
    }
    
    /**
     * 手动发送一条测试数据
     */
    public void sendTestData() {
        if (producer == null) {
            logger.info("Kafka生产者未初始化，正在初始化...");
            initProducer();
        }
        
        try {
            String sensorId = "test-sensor-001";
            ObjectNode dataNode = objectMapper.createObjectNode();
            dataNode.put("sensorId", sensorId);
            dataNode.put("sensorType", "temperature");
            dataNode.put("value", 25.5);
            dataNode.put("unit", "°C");
            dataNode.put("timestamp", System.currentTimeMillis());
            dataNode.put("location", "测试区域");
            dataNode.put("batteryLevel", 100);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
            dataNode.put("readableTime", dateFormat.format(new Date()));
            
            String jsonData = objectMapper.writeValueAsString(dataNode);
            
            logger.info("准备发送测试数据到Kafka主题 {}: {}", sensorTopic, jsonData);
            producer.send(new ProducerRecord<>(sensorTopic, sensorId, jsonData), (metadata, exception) -> {
                if (exception == null) {
                    logger.info("测试数据已发送到Kafka，主题: {}, 分区: {}, 偏移量: {}", 
                            metadata.topic(), metadata.partition(), metadata.offset());
                } else {
                    logger.error("发送测试数据到Kafka失败: {}", exception.getMessage(), exception);
                }
            });
        } catch (Exception e) {
            logger.error("发送测试数据时出错: {}", e.getMessage(), e);
        }
    }
} 