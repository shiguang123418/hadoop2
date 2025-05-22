package org.shiguang

import org.shiguang.utils.{ConfigManager, ConfigKeys}

/**
 * Spark配置类，用于加载和管理配置信息
 */
case class SparkConfig(
  sparkMaster: String,
  batchInterval: Int,
  appName: String,
  sparkUiPort: Int,
  kafkaBootstrapServers: String,
  kafkaTopic: String,
  kafkaGroupId: String,
  webSocketHost: String,
  webSocketPort: Int,
  webSocketPath: String,
  dbDriver: String,
  dbUrl: String,
  dbUser: String,
  dbPassword: String,
  thresholds: Map[String, (Double, Double)] // 保存所有的阈值配置 (最小值, 最大值)
)

object SparkConfig {
  /**
   * 从配置文件加载应用配置
   */
  def loadConfig(): SparkConfig = {
    try {
      println("正在加载配置文件...")
      
      // 使用ConfigManager加载配置
      ConfigManager.loadConfig()
      
      println("成功加载配置文件")
      
      // 加载阈值配置
      val thresholds = loadThresholds()
      
      val sparkConfig = SparkConfig(
        sparkMaster = ConfigManager.getString(ConfigKeys.Spark.MASTER),
        batchInterval = ConfigManager.getInt(ConfigKeys.Spark.BATCH_INTERVAL),
        appName = ConfigManager.getString(ConfigKeys.Spark.APP_NAME),
        sparkUiPort = ConfigManager.getInt(ConfigKeys.Spark.UI_PORT),
        kafkaBootstrapServers = ConfigManager.getString(ConfigKeys.Kafka.BOOTSTRAP_SERVERS),
        kafkaTopic = ConfigManager.getString(ConfigKeys.Kafka.TOPIC),
        kafkaGroupId = ConfigManager.getString(ConfigKeys.Kafka.GROUP_ID),
        webSocketHost = ConfigManager.getString(ConfigKeys.WebSocket.HOST),
        webSocketPort = ConfigManager.getInt(ConfigKeys.WebSocket.PORT),
        webSocketPath = ConfigManager.getString(ConfigKeys.WebSocket.PATH),
        dbDriver = ConfigManager.getString(ConfigKeys.Database.DRIVER),
        dbUrl = ConfigManager.getString(ConfigKeys.Database.URL),
        dbUser = ConfigManager.getString(ConfigKeys.Database.USER),
        dbPassword = ConfigManager.getString(ConfigKeys.Database.PASSWORD),
        thresholds = thresholds
      )
      
      // 打印关键配置，用于调试
      println(s"加载的配置: WebSocket=${sparkConfig.webSocketHost}:${sparkConfig.webSocketPort}${sparkConfig.webSocketPath}")
      println(s"Spark配置: master=${sparkConfig.sparkMaster}, appName=${sparkConfig.appName}")
      
      sparkConfig
    } catch {
      case e: Exception =>
        println(s"配置加载失败: ${e.getMessage}")
        e.printStackTrace()
        throw e
    }
  }
  
  /**
   * 加载所有阈值配置
   * @return 返回阈值映射表，格式为 Map[String, (Double, Double)]，即 (参数名, (最小值, 最大值))
   */
  private def loadThresholds(): Map[String, (Double, Double)] = {
    Map(
      "temperature" -> (
        ConfigManager.getDouble(ConfigKeys.Threshold.TEMPERATURE_MIN),
        ConfigManager.getDouble(ConfigKeys.Threshold.TEMPERATURE_MAX)
      ),
      "humidity" -> (
        ConfigManager.getDouble(ConfigKeys.Threshold.HUMIDITY_MIN),
        ConfigManager.getDouble(ConfigKeys.Threshold.HUMIDITY_MAX)
      ),
      "soilMoisture" -> (
        ConfigManager.getDouble(ConfigKeys.Threshold.SOIL_MOISTURE_MIN),
        ConfigManager.getDouble(ConfigKeys.Threshold.SOIL_MOISTURE_MAX)
      ),
      "lightIntensity" -> (
        ConfigManager.getDouble(ConfigKeys.Threshold.LIGHT_INTENSITY_MIN, 0.0),
        ConfigManager.getDouble(ConfigKeys.Threshold.LIGHT_INTENSITY_MAX, Double.MaxValue)
      ),
      "co2" -> (
        ConfigManager.getDouble(ConfigKeys.Threshold.CO2_MIN, 0.0),
        ConfigManager.getDouble(ConfigKeys.Threshold.CO2_MAX, Double.MaxValue)
      ),
      "batteryLevel" -> (
        ConfigManager.getDouble(ConfigKeys.Threshold.BATTERY_MIN, 0.0),
        Double.MaxValue
      )
    )
  }
} 