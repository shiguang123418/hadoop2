package org.shiguang.utils

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.shiguang.SparkConfig
import org.shiguang.model.SensorData
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write

import java.util.Properties
import scala.util.Random

/**
 * 传感器数据生成器，用于生成模拟的传感器数据并发送到Kafka
 */
object SensorDataGenerator {
  implicit val formats = DefaultFormats
  private val random = new Random()

  /**
   * 生成单条随机传感器数据
   */
  def generateSensorData(sensorId: String): SensorData = {
    SensorData(
      sensorId = sensorId,
      timestamp = System.currentTimeMillis(),
      temperature = 15 + random.nextDouble() * 25, // 15-40℃
      humidity = 40 + random.nextDouble() * 50, // 40-90%
      soilMoisture = 20 + random.nextDouble() * 60, // 20-80%
      lightIntensity = 300 + random.nextDouble() * 700, // 300-1000 lux
      location = s"Field-${sensorId.split("-").last}",
      batteryLevel = 50 + random.nextDouble() * 50 // 50-100%
    )
  }

  /**
   * 创建Kafka生产者
   */
  def createProducer(bootstrapServers: String): KafkaProducer[String, String] = {
    val props = new Properties()
    props.put("bootstrap.servers", bootstrapServers)
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("acks", "1")
    new KafkaProducer[String, String](props)
  }

  /**
   * 发送数据到Kafka
   */
  def sendToKafka(producer: KafkaProducer[String, String], topic: String, data: SensorData): Unit = {
    val jsonData = write(data)
    val record = new ProducerRecord[String, String](topic, data.sensorId, jsonData)
    producer.send(record)
  }

  /**
   * 生成多条数据并发送到Kafka
   */
  def generateAndSendData(count: Int, intervalMs: Int = 1000): Unit = {
    // 加载配置
    val config = SparkConfig.loadConfig()
    val producer = createProducer(config.kafkaBootstrapServers)
    
    val sensorIds = Array("sensor-001", "sensor-002", "sensor-003", "sensor-004", "sensor-005")
    
    try {
      println(s"开始生成并发送${count}条传感器数据，间隔${intervalMs}ms...")
      
      for (i <- 1 to count) {
        val sensorId = sensorIds(random.nextInt(sensorIds.length))
        val sensorData = generateSensorData(sensorId)
        
        sendToKafka(producer, config.kafkaTopic, sensorData)
        
        println(s"已发送数据(${i}/${count}): ${sensorData}")
        
        if (i < count) {
          Thread.sleep(intervalMs)
        }
      }
      
      println("数据发送完成")
    } catch {
      case e: Exception =>
        println(s"发送数据时出错: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      producer.close()
    }
  }

  /**
   * 主方法，用于测试数据生成
   */
  def main(args: Array[String]): Unit = {
    val count = if (args.length > 0) args(0).toInt else 10
    val interval = if (args.length > 1) args(1).toInt else 1000
    
    generateAndSendData(count, interval)
  }
} 