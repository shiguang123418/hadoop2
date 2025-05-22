package org.shiguang.utils

/**
 * 配置键常量类
 * 定义所有配置项的路径，避免硬编码字符串
 */
object ConfigKeys {
  // Spark 配置
  object Spark {
    val MASTER = "spark.master"
    val APP_NAME = "spark.app-name"
    val BATCH_INTERVAL = "spark.batch-interval"
    val UI_PORT = "spark.ui.port"
    val UI_ENABLED = "spark.ui.enabled"
    val STREAMING_BATCH_DURATION = "spark.streaming.batch-duration"
    val KRYO_ENABLED = "spark.kryo.enabled"
    val KRYO_BUFFER_MAX_MB = "spark.kryo.buffer-max-mb"
  }
  
  // Kafka 配置
  object Kafka {
    val BOOTSTRAP_SERVERS = "kafka.bootstrap-servers"
    val TOPIC = "kafka.topic"
    val GROUP_ID = "kafka.group-id"
  }
  
  // WebSocket 配置
  object WebSocket {
    val HOST = "websocket.host"
    val PORT = "websocket.port"
    val PATH = "websocket.path"
  }
  
  // 数据库配置
  object Database {
    val DRIVER = "database.driver"
    val URL = "database.url"
    val USER = "database.user"
    val PASSWORD = "database.password"
  }
  
  // 阈值配置
  object Threshold {
    val TEMPERATURE_MAX = "threshold.temperature.max"
    val TEMPERATURE_MIN = "threshold.temperature.min"
    val HUMIDITY_MAX = "threshold.humidity.max"
    val HUMIDITY_MIN = "threshold.humidity.min"
    val SOIL_MOISTURE_MAX = "threshold.soil-moisture.max"
    val SOIL_MOISTURE_MIN = "threshold.soil-moisture.min"
    val LIGHT_INTENSITY_MAX = "threshold.light-intensity.max"
    val LIGHT_INTENSITY_MIN = "threshold.light-intensity.min"
    val CO2_MAX = "threshold.co2.max"
    val CO2_MIN = "threshold.co2.min"
    val BATTERY_MIN = "threshold.battery.min"
  }
  
  // 日志配置
  object Logging {
    val ROOT_LEVEL = "logging.level.root"
    val APP_LEVEL = "logging.level.org.shiguang"
    val SPARK_LEVEL = "logging.level.org.apache.spark"
    val KAFKA_LEVEL = "logging.level.org.apache.kafka"
  }
  
  // 主机配置
  object Hosts {
    val SPARK_HOST = "hosts.spark"
    val KAFKA_HOST = "hosts.kafka"
    val MYSQL_HOST = "hosts.mysql"
  }
} 