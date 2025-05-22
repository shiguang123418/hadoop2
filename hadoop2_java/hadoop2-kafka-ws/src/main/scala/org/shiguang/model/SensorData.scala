package org.shiguang.model

import org.shiguang.SparkConfig
import org.shiguang.db.DbConnection
import org.shiguang.utils.{ConfigManager, ConfigKeys}

import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.time.Instant

/**
 * 农业传感器数据模型
 */
case class SensorData(
  sensorId: String,
  timestamp: Long,
  temperature: Double,
  humidity: Double,
  soilMoisture: Double,
  lightIntensity: Double,
  location: String,
  batteryLevel: Double
) {

  /**
   * 保存传感器数据到数据库
   * 为了适应现有的数据库结构，我们需要将多种类型的数据分别保存
   */
  def saveToDatabase(): Unit = {
    var connection: Connection = null
    
    try {
      connection = DbConnection.getConnection()
      
      // 保存温度数据
      saveDataPoint(connection, "temperature", temperature, "°C")
      
      // 保存湿度数据
      saveDataPoint(connection, "humidity", humidity, "%")
      
      // 保存土壤湿度数据
      saveDataPoint(connection, "soil_moisture", soilMoisture, "%")
      
      // 保存光照强度数据
      saveDataPoint(connection, "light_intensity", lightIntensity, "lux")
      
      // 保存电池电量数据
      saveDataPoint(connection, "battery_level", batteryLevel, "%")
      
      println(s"成功保存传感器数据: $sensorId")
      
    } catch {
      case e: Exception =>
        println(s"保存数据到数据库时出错: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      if (connection != null) {
        connection.close()
      }
    }
  }
  
  /**
   * 保存单个数据点到数据库
   */
  private def saveDataPoint(connection: Connection, sensorType: String, value: Double, unit: String): Unit = {
    var preparedStatement: PreparedStatement = null
    
    try {
      val sql = """
        INSERT INTO sensor_data 
        (sensor_id, timestamp, sensor_type, value, unit, location, is_alert, status) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      """
      
      // 检测是否有异常
      val anomalies = detectAnomalies()
      val isAlert = anomalies.exists(_._1 == sensorType)
      
      preparedStatement = connection.prepareStatement(sql)
      preparedStatement.setString(1, sensorId)
      preparedStatement.setTimestamp(2, Timestamp.from(Instant.ofEpochMilli(timestamp)))
      preparedStatement.setString(3, sensorType)
      preparedStatement.setDouble(4, value)
      preparedStatement.setString(5, unit)
      preparedStatement.setString(6, location)
      preparedStatement.setBoolean(7, isAlert)
      preparedStatement.setString(8, if (isAlert) "异常" else "正常")
      
      preparedStatement.executeUpdate()
      
    } catch {
      case e: Exception =>
        println(s"保存 $sensorType 数据时出错: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      if (preparedStatement != null) {
        preparedStatement.close()
      }
    }
  }
  
  /**
   * 检测传感器数据是否有异常
   * @return 检测到的异常列表，如无异常则返回空列表
   */
  def detectAnomalies(): List[(String, Double, Double, Double)] = {
    val anomalies = collection.mutable.ListBuffer[(String, Double, Double, Double)]()
    
    // 获取阈值配置
    val tempMin = ConfigManager.getDouble(ConfigKeys.Threshold.TEMPERATURE_MIN)
    val tempMax = ConfigManager.getDouble(ConfigKeys.Threshold.TEMPERATURE_MAX)
    val humidityMin = ConfigManager.getDouble(ConfigKeys.Threshold.HUMIDITY_MIN)
    val humidityMax = ConfigManager.getDouble(ConfigKeys.Threshold.HUMIDITY_MAX)
    val soilMoistureMin = ConfigManager.getDouble(ConfigKeys.Threshold.SOIL_MOISTURE_MIN)
    val soilMoistureMax = ConfigManager.getDouble(ConfigKeys.Threshold.SOIL_MOISTURE_MAX)
    val lightIntensityMin = ConfigManager.getDouble(ConfigKeys.Threshold.LIGHT_INTENSITY_MIN, 0.0)
    val lightIntensityMax = ConfigManager.getDouble(ConfigKeys.Threshold.LIGHT_INTENSITY_MAX, Double.MaxValue)
    val co2Min = ConfigManager.getDouble(ConfigKeys.Threshold.CO2_MIN, 0.0)
    val co2Max = ConfigManager.getDouble(ConfigKeys.Threshold.CO2_MAX, Double.MaxValue)
    val batteryMin = ConfigManager.getDouble(ConfigKeys.Threshold.BATTERY_MIN, 0.0)
    
    // 检查温度异常
    if (temperature < tempMin || temperature > tempMax) {
      anomalies += (("temperature", temperature, tempMin, tempMax))
    }
    
    // 检查湿度异常
    if (humidity < humidityMin || humidity > humidityMax) {
      anomalies += (("humidity", humidity, humidityMin, humidityMax))
    }
    
    // 检查土壤湿度异常
    if (soilMoisture < soilMoistureMin || soilMoisture > soilMoistureMax) {
      anomalies += (("soilMoisture", soilMoisture, soilMoistureMin, soilMoistureMax))
    }
    
    // 检查光照强度异常
    if (lightIntensity < lightIntensityMin || lightIntensity > lightIntensityMax) {
      anomalies += (("lightIntensity", lightIntensity, lightIntensityMin, lightIntensityMax))
    }
    
    // 检查电池电量异常
    if (batteryLevel < batteryMin) {
      anomalies += (("batteryLevel", batteryLevel, batteryMin, Double.MaxValue))
    }
    
    anomalies.toList
  }
}

object SensorData {
  /**
   * 查询指定时间范围内的传感器数据
   */
  def queryByTimeRange(sensorId: String, startTime: Long, endTime: Long): List[SensorData] = {
    var connection: Connection = null
    var preparedStatement: PreparedStatement = null
    var resultSet: ResultSet = null
    val results = collection.mutable.ListBuffer[SensorData]()
    
    try {
      connection = DbConnection.getConnection()
      val sql = """
        SELECT sensor_id, timestamp, temperature, humidity, soil_moisture, light_intensity, location, battery_level 
        FROM sensor_data 
        WHERE sensor_id = ? AND timestamp BETWEEN ? AND ? 
        ORDER BY timestamp DESC
      """
      
      preparedStatement = connection.prepareStatement(sql)
      preparedStatement.setString(1, sensorId)
      preparedStatement.setTimestamp(2, Timestamp.from(Instant.ofEpochMilli(startTime)))
      preparedStatement.setTimestamp(3, Timestamp.from(Instant.ofEpochMilli(endTime)))
      
      resultSet = preparedStatement.executeQuery()
      
      while (resultSet.next()) {
        results += SensorData(
          resultSet.getString("sensor_id"),
          resultSet.getTimestamp("timestamp").getTime,
          resultSet.getDouble("temperature"),
          resultSet.getDouble("humidity"),
          resultSet.getDouble("soil_moisture"),
          resultSet.getDouble("light_intensity"),
          resultSet.getString("location"),
          resultSet.getDouble("battery_level")
        )
      }
      
    } catch {
      case e: Exception =>
        println(s"查询传感器数据时出错: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      if (resultSet != null) {
        resultSet.close()
      }
      if (preparedStatement != null) {
        preparedStatement.close()
      }
      if (connection != null) {
        connection.close()
      }
    }
    
    results.toList
  }
} 