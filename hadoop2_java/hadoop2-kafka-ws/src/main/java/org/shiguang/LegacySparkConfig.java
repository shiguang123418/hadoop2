package org.shiguang;

import org.shiguang.utils.ConfigManager;
import org.shiguang.utils.ConfigKeys;
import java.util.HashMap;
import java.util.Map;

/**
 * 传统Spark配置类（已废弃）
 * @deprecated 请使用Spring管理的配置类 {@link org.shiguang.config.SparkConfig}
 * 此类将在未来版本中移除
 */
@Deprecated
public class LegacySparkConfig {
    private String sparkMaster;
    private int batchInterval;
    private String appName;
    private int sparkUiPort;
    private String kafkaBootstrapServers;
    private String kafkaTopic;
    private String kafkaGroupId;
    private String webSocketHost;
    private int webSocketPort;
    private String webSocketPath;
    private String dbDriver;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private Map<String, Threshold> thresholds;  // 阈值配置
    
    // 内部类表示阈值配置
    public static class Threshold {
        private double min;
        private double max;
        
        public Threshold(double min, double max) {
            this.min = min;
            this.max = max;
        }
        
        public double getMin() {
            return min;
        }
        
        public double getMax() {
            return max;
        }
    }
    
    // 构造函数为私有，使用loadConfig工厂方法创建实例
    private LegacySparkConfig() {
    }
    
    /**
     * 从配置文件加载应用配置
     */
    public static LegacySparkConfig loadConfig() {
        try {
            System.out.println("正在加载配置文件...");
            
            // 使用ConfigManager加载配置
            ConfigManager.loadConfig();
            
            System.out.println("成功加载配置文件");
            
            // 创建SparkConfig实例
            LegacySparkConfig sparkConfig = new LegacySparkConfig();
            
            // 加载配置项
            sparkConfig.sparkMaster = ConfigManager.getString(ConfigKeys.Spark.MASTER);
            sparkConfig.batchInterval = ConfigManager.getInt(ConfigKeys.Spark.BATCH_INTERVAL);
            sparkConfig.appName = ConfigManager.getString(ConfigKeys.Spark.APP_NAME);
            sparkConfig.sparkUiPort = ConfigManager.getInt(ConfigKeys.Spark.UI_PORT);
            sparkConfig.kafkaBootstrapServers = ConfigManager.getString(ConfigKeys.Kafka.BOOTSTRAP_SERVERS);
            sparkConfig.kafkaTopic = ConfigManager.getString(ConfigKeys.Kafka.TOPIC);
            sparkConfig.kafkaGroupId = ConfigManager.getString(ConfigKeys.Kafka.GROUP_ID);
            sparkConfig.webSocketHost = ConfigManager.getString(ConfigKeys.WebSocket.HOST);
            sparkConfig.webSocketPort = ConfigManager.getInt(ConfigKeys.WebSocket.PORT);
            sparkConfig.webSocketPath = ConfigManager.getString(ConfigKeys.WebSocket.PATH);
            sparkConfig.dbDriver = ConfigManager.getString(ConfigKeys.Database.DRIVER);
            sparkConfig.dbUrl = ConfigManager.getString(ConfigKeys.Database.URL);
            sparkConfig.dbUser = ConfigManager.getString(ConfigKeys.Database.USER);
            sparkConfig.dbPassword = ConfigManager.getString(ConfigKeys.Database.PASSWORD);
            
            // 加载阈值配置
            sparkConfig.thresholds = loadThresholds();
            
            // 打印关键配置，用于调试
            System.out.println("加载的配置: WebSocket=" + sparkConfig.webSocketHost + ":" + 
                              sparkConfig.webSocketPort + sparkConfig.webSocketPath);
            System.out.println("Spark配置: master=" + sparkConfig.sparkMaster + 
                              ", appName=" + sparkConfig.appName);
            
            return sparkConfig;
        } catch (Exception e) {
            System.out.println("配置加载失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * 加载所有阈值配置
     */
    private static Map<String, Threshold> loadThresholds() {
        Map<String, Threshold> thresholds = new HashMap<>();
        
        // 加载温度阈值
        thresholds.put("temperature", new Threshold(
            ConfigManager.getDouble(ConfigKeys.Threshold.TEMPERATURE_MIN),
            ConfigManager.getDouble(ConfigKeys.Threshold.TEMPERATURE_MAX)
        ));
        
        // 加载湿度阈值
        thresholds.put("humidity", new Threshold(
            ConfigManager.getDouble(ConfigKeys.Threshold.HUMIDITY_MIN),
            ConfigManager.getDouble(ConfigKeys.Threshold.HUMIDITY_MAX)
        ));
        
        // 加载土壤湿度阈值
        thresholds.put("soilMoisture", new Threshold(
            ConfigManager.getDouble(ConfigKeys.Threshold.SOIL_MOISTURE_MIN),
            ConfigManager.getDouble(ConfigKeys.Threshold.SOIL_MOISTURE_MAX)
        ));
        
        // 加载光照强度阈值
        thresholds.put("lightIntensity", new Threshold(
            ConfigManager.getDouble(ConfigKeys.Threshold.LIGHT_INTENSITY_MIN, 0.0),
            ConfigManager.getDouble(ConfigKeys.Threshold.LIGHT_INTENSITY_MAX, Double.MAX_VALUE)
        ));
        
        // 加载CO2浓度阈值
        thresholds.put("co2", new Threshold(
            ConfigManager.getDouble(ConfigKeys.Threshold.CO2_MIN, 0.0),
            ConfigManager.getDouble(ConfigKeys.Threshold.CO2_MAX, Double.MAX_VALUE)
        ));
        
        // 加载电池电量阈值
        thresholds.put("batteryLevel", new Threshold(
            ConfigManager.getDouble(ConfigKeys.Threshold.BATTERY_MIN, 0.0),
            Double.MAX_VALUE
        ));
        
        return thresholds;
    }
    
    // Getter方法
    public String getSparkMaster() {
        return sparkMaster;
    }
    
    public int getBatchInterval() {
        return batchInterval;
    }
    
    public String getAppName() {
        return appName;
    }
    
    public int getSparkUiPort() {
        return sparkUiPort;
    }
    
    public String getKafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }
    
    public String getKafkaTopic() {
        return kafkaTopic;
    }
    
    public String getKafkaGroupId() {
        return kafkaGroupId;
    }
    
    public String getWebSocketHost() {
        return webSocketHost;
    }
    
    public int getWebSocketPort() {
        return webSocketPort;
    }
    
    public String getWebSocketPath() {
        return webSocketPath;
    }
    
    public String getDbDriver() {
        return dbDriver;
    }
    
    public String getDbUrl() {
        return dbUrl;
    }
    
    public String getDbUser() {
        return dbUser;
    }
    
    public String getDbPassword() {
        return dbPassword;
    }
    
    public Map<String, Threshold> getThresholds() {
        return thresholds;
    }
} 