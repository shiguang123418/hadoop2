package org.shiguang.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用程序配置属性类
 * 映射application.yml中的app配置
 */
@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    
    private SparkProperties spark;
    private KafkaProperties kafka;
    private WebSocketProperties websocket;
    private ThresholdProperties threshold;
    
    @Data
    public static class SparkProperties {
        private String master;
        private int batchInterval;
        private UiProperties ui;
        
        @Data
        public static class UiProperties {
            private int port;
            private boolean enabled;
        }
    }
    
    @Data
    public static class KafkaProperties {
        private String topic;
    }
    
    @Data
    public static class WebSocketProperties {
        private String host;
        private int port;
        private String path;
    }
    
    @Data
    public static class ThresholdProperties {
        private RangeProperties temperature;
        private RangeProperties humidity;
        private RangeProperties soilMoisture;
        private RangeProperties lightIntensity;
        private RangeProperties co2;
        private BatteryProperties battery;
        
        @Data
        public static class RangeProperties {
            private double min;
            private double max;
        }
        
        @Data
        public static class BatteryProperties {
            private double min;
        }
    }
} 