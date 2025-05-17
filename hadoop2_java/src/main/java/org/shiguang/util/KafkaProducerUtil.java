package org.shiguang.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.shiguang.config.KafkaEnabledCondition;
import org.shiguang.model.SensorData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

/**
 * Kafka生产者工具类，用于生成测试数据
 */
@Component
@Conditional(KafkaEnabledCondition.class)
public class KafkaProducerUtil {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerUtil.class);
    protected final ObjectMapper objectMapper;
    protected final Random random = new Random();
    
    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    @Value("${kafka.topics.sensor-data:agriculture-sensor-data}")
    private String sensorDataTopic;

    public KafkaProducerUtil() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 创建Kafka生产者配置
     */
    private Properties createProducerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        return props;
    }

    /**
     * 生成并发送一条随机传感器数据到Kafka
     * 
     * @return 生成的传感器数据
     */
    public SensorData sendRandomSensorData() {
        try (Producer<String, String> producer = new KafkaProducer<>(createProducerProps())) {
            SensorData sensorData = generateRandomSensorData();
            String jsonData = objectMapper.writeValueAsString(sensorData);
            
            ProducerRecord<String, String> record = 
                    new ProducerRecord<>(sensorDataTopic, UUID.randomUUID().toString(), jsonData);
            
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("发送数据到Kafka失败", exception);
                } else {
                    logger.info("成功发送数据到Kafka: topic={}, partition={}, offset={}", 
                            metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
            
            return sensorData;
        } catch (JsonProcessingException e) {
            logger.error("序列化传感器数据失败", e);
            return null;
        }
    }

    /**
     * 生成指定数量的随机传感器数据并发送到Kafka
     * 
     * @param count 数据条数
     */
    public void sendMultipleRandomSensorData(int count) {
        try (Producer<String, String> producer = new KafkaProducer<>(createProducerProps())) {
            for (int i = 0; i < count; i++) {
                SensorData sensorData = generateRandomSensorData();
                try {
                    String jsonData = objectMapper.writeValueAsString(sensorData);
                    ProducerRecord<String, String> record = 
                            new ProducerRecord<>(sensorDataTopic, UUID.randomUUID().toString(), jsonData);
                    
                    producer.send(record);
                    Thread.sleep(100); // 稍微延迟以模拟实时数据
                } catch (Exception e) {
                    logger.error("发送第 {} 条数据失败", i, e);
                }
            }
            logger.info("成功发送 {} 条随机传感器数据到Kafka", count);
        } catch (Exception e) {
            logger.error("批量发送数据失败", e);
        }
    }

    /**
     * 生成一条随机的传感器数据
     * 
     * @return 随机生成的传感器数据
     */
    protected SensorData generateRandomSensorData() {
        String[] sensorTypes = {"温度", "湿度", "光照", "土壤湿度", "pH值"};
        String[] regions = {"华北", "华东", "华南", "西北", "西南", "东北", "华中"};
        String[] cropTypes = {"小麦", "水稻", "玉米", "大豆", "棉花", "马铃薯", "甜菜"};
        
        SensorData data = new SensorData();
        String sensorType = sensorTypes[random.nextInt(sensorTypes.length)];
        
        data.setSensorId("SENSOR-" + (1000 + random.nextInt(9000)));
        data.setSensorType(sensorType);
        data.setRegion(regions[random.nextInt(regions.length)]);
        data.setCropType(cropTypes[random.nextInt(cropTypes.length)]);
        data.setUnit(getUnitForSensorType(sensorType));
        
        // 根据传感器类型设置合理的值范围
        data.setValue(getRandomValueForSensorType(sensorType));
        
        data.setTimestamp(LocalDateTime.now());
        data.setDescription(sensorType + "传感器读数 - " + data.getRegion() + " - " + data.getCropType());
        
        return data;
    }

    /**
     * 根据传感器类型获取对应的单位
     */
    protected String getUnitForSensorType(String type) {
        switch (type) {
            case "温度": return "°C";
            case "湿度": return "%";
            case "光照": return "lux";
            case "土壤湿度": return "%";
            case "pH值": return "";
            default: return "";
        }
    }

    /**
     * 根据传感器类型获取合理的随机值
     */
    protected double getRandomValueForSensorType(String type) {
        switch (type) {
            case "温度": return 10 + random.nextDouble() * 30; // 10-40°C
            case "湿度": return 30 + random.nextDouble() * 70; // 30-100%
            case "光照": return 1000 + random.nextDouble() * 90000; // 1000-100000 lux
            case "土壤湿度": return 20 + random.nextDouble() * 60; // 20-80%
            case "pH值": return 4 + random.nextDouble() * 6; // 4-10
            default: return random.nextDouble() * 100;
        }
    }
} 