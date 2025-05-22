package org.shiguang.db

import org.shiguang.utils.{ConfigManager, ConfigKeys}
import java.sql.{Connection, DriverManager}

/**
 * 数据库连接工具类
 */
object DbConnection {
  private var dbDriver: String = _
  private var dbUrl: String = _
  private var dbUser: String = _
  private var dbPassword: String = _
  
  // 初始化数据库配置
  private def initDbConfig(): Unit = {
    if (dbDriver == null) {
      dbDriver = ConfigManager.getString(ConfigKeys.Database.DRIVER)
      dbUrl = ConfigManager.getString(ConfigKeys.Database.URL)
      dbUser = ConfigManager.getString(ConfigKeys.Database.USER)
      dbPassword = ConfigManager.getString(ConfigKeys.Database.PASSWORD)
    }
  }
  
  /**
   * 获取数据库连接
   */
  def getConnection(): Connection = {
    initDbConfig()
    
    try {
      Class.forName(dbDriver)
      DriverManager.getConnection(dbUrl, dbUser, dbPassword)
    } catch {
      case e: Exception =>
        println(s"获取数据库连接时出错: ${e.getMessage}")
        e.printStackTrace()
        throw e
    }
  }
  
  /**
   * 测试数据库连接
   */
  def testConnection(): Boolean = {
    var connection: Connection = null
    try {
      connection = getConnection()
      true
    } catch {
      case _: Exception => false
    } finally {
      if (connection != null) {
        connection.close()
      }
    }
  }
  
  /**
   * 初始化数据库表
   */
  def initDatabase(): Unit = {
    var connection: Connection = null
    var statement: java.sql.Statement = null
    
    try {
      connection = getConnection()
      statement = connection.createStatement()
      
      // 创建传感器数据表
      val createTableSQL = """
        CREATE TABLE IF NOT EXISTS sensor_data (
          id BIGINT AUTO_INCREMENT PRIMARY KEY,
          sensor_id VARCHAR(50) NOT NULL,
          timestamp TIMESTAMP NOT NULL,
          temperature DOUBLE NOT NULL,
          humidity DOUBLE NOT NULL,
          soil_moisture DOUBLE NOT NULL,
          light_intensity DOUBLE NOT NULL,
          location VARCHAR(100) NOT NULL,
          battery_level DOUBLE NOT NULL,
          INDEX idx_sensor_time (sensor_id, timestamp)
        )
      """
      
      statement.execute(createTableSQL)
      println("数据库表初始化成功")
      
    } catch {
      case e: Exception =>
        println(s"初始化数据库时出错: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      if (statement != null) {
        statement.close()
      }
      if (connection != null) {
        connection.close()
      }
    }
  }
} 