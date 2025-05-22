package org.shiguang

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.kafka.common.serialization.StringDeserializer
import org.shiguang.websocket.WebSocketServer
import org.shiguang.model.SensorData
import org.json4s._
import org.json4s.jackson.JsonMethods._

object AgricultureSensorApp {
  // 设置JSON格式化
  implicit val formats = DefaultFormats

  def main(args: Array[String]): Unit = {
    // 加载配置
    val config = SparkConfig.loadConfig()
    
    // 创建Spark配置
    val sparkConf = new SparkConf()
      .setAppName(config.appName)
      .setMaster(config.sparkMaster)
      .set("spark.streaming.stopGracefullyOnShutdown", "true")
      .set("spark.ui.enabled", "true")
      .set("spark.ui.port", config.sparkUiPort.toString)
    
    // 创建StreamingContext
    val ssc = new StreamingContext(sparkConf, Seconds(config.batchInterval))
    
    // 设置日志级别
    ssc.sparkContext.setLogLevel("WARN")
    
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
    
    // 启动WebSocket服务器
    val webSocketServer = new WebSocketServer(
      config.webSocketHost,
      config.webSocketPort,
      config.webSocketPath
    )
    webSocketServer.start()
    println(s"WebSocket服务器已启动，监听地址: ${config.webSocketHost}:${config.webSocketPort}, 路径: ${config.webSocketPath}")
    
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
              
              // 发送数据到WebSocket客户端
              webSocketServer.broadcast(record.value())
              
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
      webSocketServer.stop()
      ssc.stop(stopSparkContext = true, stopGracefully = true)
      println("应用已关闭")
    }
    
    // 等待处理终止
    ssc.awaitTermination()
  }
} 