package org.shiguang

import com.typesafe.config.{Config, ConfigFactory}
import java.io.File

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
  dbPassword: String
)

object SparkConfig {
  /**
   * 从配置文件加载应用配置
   */
  def loadConfig(): SparkConfig = {
    try {
      val config = ConfigFactory.load()
      println("成功加载配置文件")
      
      val sparkConfig = SparkConfig(
        sparkMaster = config.getString("spark.master"),
        batchInterval = config.getInt("spark.batch-interval"),
        appName = config.getString("spark.app-name"),
        sparkUiPort = config.getInt("spark.ui.port"),
        kafkaBootstrapServers = config.getString("kafka.bootstrap-servers"),
        kafkaTopic = config.getString("kafka.topic"),
        kafkaGroupId = config.getString("kafka.group-id"),
        webSocketHost = config.getString("websocket.host"),
        webSocketPort = config.getInt("websocket.port"),
        webSocketPath = config.getString("websocket.path"),
        dbDriver = config.getString("database.driver"),
        dbUrl = config.getString("database.url"),
        dbUser = config.getString("database.user"),
        dbPassword = config.getString("database.password")
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
} 