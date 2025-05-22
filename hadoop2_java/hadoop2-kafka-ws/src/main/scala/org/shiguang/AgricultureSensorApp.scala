package org.shiguang

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.kafka.common.serialization.StringDeserializer
import org.shiguang.websocket.WebSocketServer
import org.shiguang.model.SensorData
import org.shiguang.utils.{ConfigManager, ConfigKeys}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.write

// 创建WebSocketServer的单例对象，避免序列化问题
object WebSocketServerSingleton {
  private var instance: WebSocketServer = _
  
  def initialize(host: String, port: Int, path: String): WebSocketServer = {
    if (instance == null) {
      instance = new WebSocketServer(host, port, path)
      instance.start()
      println(s"WebSocket服务器已启动，监听地址: $host:$port, 路径: $path")
    }
    instance
  }
  
  def getInstance: WebSocketServer = instance
  
  def stop(): Unit = {
    if (instance != null) {
      instance.stop()
      instance = null
    }
  }
}

object AgricultureSensorApp {
  // 设置JSON格式化
  implicit val formats = DefaultFormats

  def main(args: Array[String]): Unit = {
    // 加载配置
    println("正在初始化配置...")
    val config = SparkConfig.loadConfig()
    
    // 创建Spark配置
    val sparkConf = new SparkConf()
      .setAppName(config.appName)
      .setMaster(config.sparkMaster)
      .set("spark.streaming.stopGracefullyOnShutdown", "true")
      .set("spark.ui.enabled", ConfigManager.getBoolean(ConfigKeys.Spark.UI_ENABLED).toString)
      .set("spark.ui.port", config.sparkUiPort.toString)
    
    // 创建StreamingContext
    val ssc = new StreamingContext(sparkConf, Seconds(config.batchInterval))
    
    // 设置日志级别
    ssc.sparkContext.setLogLevel(ConfigManager.getString(ConfigKeys.Logging.ROOT_LEVEL))
    
    println("正在启动Kafka消费者...")
    
    // Kafka 消费者配置
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> config.kafkaBootstrapServers,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> config.kafkaGroupId,
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )
    
    // 创建直接流
    val topics = Array(config.kafkaTopic)
    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](topics, kafkaParams)
    )
    
    // 启动WebSocket服务器（使用单例模式）
    WebSocketServerSingleton.initialize(
      config.webSocketHost,
      config.webSocketPort,
      config.webSocketPath
    )
    println(s"当前环境: ${ConfigManager.getActiveProfile}")
    
    // 处理数据流
    stream.foreachRDD { rdd =>
      // 仅当有数据时处理
      if (!rdd.isEmpty()) {
        println("接收到新的数据批次...")
        
        // 处理每条消息
        rdd.foreachPartition { partition =>
          partition.foreach { record: ConsumerRecord[String, String] =>
            try {
              // 解析JSON消息
              val json = parse(record.value())
              val sensorData = json.extract[SensorData]
              
              // 检测数据异常
              val anomalies = sensorData.detectAnomalies()
              
              // 获取WebSocket实例并发送数据
              WebSocketServerSingleton.getInstance.broadcastSensorData(sensorData)
              
              // 如果有异常，发送异常警报
              if (anomalies.nonEmpty) {
                val anomalyData = Map(
                  "type" -> "anomaly",
                  "sensorId" -> sensorData.sensorId,
                  "timestamp" -> sensorData.timestamp,
                  "anomalies" -> anomalies.map { case (param, value, min, max) =>
                    Map(
                      "parameter" -> param,
                      "value" -> value,
                      "min" -> min,
                      "max" -> max
                    )
                  }
                )
                
                // 序列化异常数据为JSON
                val anomalyJson = write(anomalyData)
                
                // 广播异常数据
                WebSocketServerSingleton.getInstance.broadcast(anomalyJson)
                println(s"检测到异常: $anomalyJson")
              }
              
              // 保存到数据库
              sensorData.saveToDatabase()
              
            } catch {
              case e: Exception => 
                println(s"处理消息时出错: ${e.getMessage}")
                e.printStackTrace()
            }
          }
        }
      }
    }
    
    // 启动Spark Streaming
    println("启动Spark Streaming...")
    ssc.start()
    
    // 添加关闭钩子
    sys.addShutdownHook {
      println("关闭应用...")
      WebSocketServerSingleton.stop()
      ssc.stop(stopSparkContext = true, stopGracefully = true)
      println("应用已关闭")
    }
    
    // 等待处理终止
    ssc.awaitTermination()
  }
} 