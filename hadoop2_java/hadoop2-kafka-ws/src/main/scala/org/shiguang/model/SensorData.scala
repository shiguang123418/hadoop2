package org.shiguang.model

import org.shiguang.SparkConfig
import org.shiguang.db.DbConnection

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
   */
  def saveToDatabase(): Unit = {
    var connection: Connection = null
    var preparedStatement: PreparedStatement = null
    
    try {
      connection = DbConnection.getConnection()
      val sql = """
        INSERT INTO sensor_data 
        (sensor_id, timestamp, temperature, humidity, soil_moisture, light_intensity, location, battery_level) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      """
      
      preparedStatement = connection.prepareStatement(sql)
      preparedStatement.setString(1, sensorId)
      preparedStatement.setTimestamp(2, Timestamp.from(Instant.ofEpochMilli(timestamp)))
      preparedStatement.setDouble(3, temperature)
      preparedStatement.setDouble(4, humidity)
      preparedStatement.setDouble(5, soilMoisture)
      preparedStatement.setDouble(6, lightIntensity)
      preparedStatement.setString(7, location)
      preparedStatement.setDouble(8, batteryLevel)
      
      val rowsAffected = preparedStatement.executeUpdate()
      if (rowsAffected > 0) {
        println(s"成功保存传感器数据: $sensorId")
      } else {
        println(s"保存传感器数据失败: $sensorId")
      }
      
    } catch {
      case e: Exception =>
        println(s"保存数据到数据库时出错: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      if (preparedStatement != null) {
        preparedStatement.close()
      }
      if (connection != null) {
        connection.close()
      }
    }
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