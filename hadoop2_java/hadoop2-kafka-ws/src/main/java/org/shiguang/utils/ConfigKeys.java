package org.shiguang.utils;

/**
 * 配置键常量类
 * 定义所有配置项的路径，避免硬编码字符串
 */
public class ConfigKeys {
    
    // Spark 配置
    public static class Spark {
        public static final String MASTER = "spark.master";
        public static final String APP_NAME = "spark.app-name";
        public static final String BATCH_INTERVAL = "spark.batch-interval";
        public static final String UI_PORT = "spark.ui.port";
        public static final String UI_ENABLED = "spark.ui.enabled";
        public static final String STREAMING_BATCH_DURATION = "spark.streaming.batch-duration";
        public static final String KRYO_ENABLED = "spark.kryo.enabled";
        public static final String KRYO_BUFFER_MAX_MB = "spark.kryo.buffer-max-mb";
    }
    
    // Kafka 配置
    public static class Kafka {
        public static final String BOOTSTRAP_SERVERS = "kafka.bootstrap-servers";
        public static final String TOPIC = "kafka.topic";
        public static final String GROUP_ID = "kafka.group-id";
    }
    
    // WebSocket 配置
    public static class WebSocket {
        public static final String HOST = "websocket.host";
        public static final String PORT = "websocket.port";
        public static final String PATH = "websocket.path";
    }
    
    // 数据库配置
    public static class Database {
        public static final String DRIVER = "database.driver";
        public static final String URL = "database.url";
        public static final String USER = "database.user";
        public static final String PASSWORD = "database.password";
    }
    
    // 阈值配置
    public static class Threshold {
        public static final String TEMPERATURE_MAX = "threshold.temperature.max";
        public static final String TEMPERATURE_MIN = "threshold.temperature.min";
        public static final String HUMIDITY_MAX = "threshold.humidity.max";
        public static final String HUMIDITY_MIN = "threshold.humidity.min";
        public static final String SOIL_MOISTURE_MAX = "threshold.soil-moisture.max";
        public static final String SOIL_MOISTURE_MIN = "threshold.soil-moisture.min";
        public static final String LIGHT_INTENSITY_MAX = "threshold.light-intensity.max";
        public static final String LIGHT_INTENSITY_MIN = "threshold.light-intensity.min";
        public static final String CO2_MAX = "threshold.co2.max";
        public static final String CO2_MIN = "threshold.co2.min";
        public static final String BATTERY_MIN = "threshold.battery.min";
    }
    
    // 日志配置
    public static class Logging {
        public static final String ROOT_LEVEL = "logging.level.root";
        public static final String APP_LEVEL = "logging.level.org.shiguang";
        public static final String SPARK_LEVEL = "logging.level.org.apache.spark";
        public static final String KAFKA_LEVEL = "logging.level.org.apache.kafka";
    }
    
    // 主机配置
    public static class Hosts {
        public static final String SPARK_HOST = "hosts.spark";
        public static final String KAFKA_HOST = "hosts.kafka";
        public static final String MYSQL_HOST = "hosts.mysql";
    }
} 