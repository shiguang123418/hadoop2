package org.shiguang.spark

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.shiguang.model.SensorData

/**
 * 传感器数据处理类，提供Spark SQL分析功能
 */
object SensorDataProcessor {

  /**
   * 创建Spark会话
   */
  def createSparkSession(appName: String, master: String): SparkSession = {
    SparkSession.builder()
      .appName(appName)
      .master(master)
      .config("spark.sql.shuffle.partitions", "4") // 小数据量时减少分区数提高性能
      .getOrCreate()
  }

  /**
   * 将传感器数据列表转换为DataFrame
   */
  def convertToDataFrame(spark: SparkSession, sensorDataList: List[SensorData]): DataFrame = {
    import spark.implicits._
    sensorDataList.toDF()
  }

  /**
   * 计算每个传感器的温度平均值
   */
  def calculateAverageTemperature(dataFrame: DataFrame): DataFrame = {
    dataFrame.groupBy("sensorId")
      .agg(
        avg("temperature").as("avgTemperature"),
        count("*").as("readingCount")
      )
      .orderBy(desc("avgTemperature"))
  }

  /**
   * 计算每个传感器的湿度平均值
   */
  def calculateAverageHumidity(dataFrame: DataFrame): DataFrame = {
    dataFrame.groupBy("sensorId")
      .agg(
        avg("humidity").as("avgHumidity"),
        count("*").as("readingCount")
      )
      .orderBy(desc("avgHumidity"))
  }

  /**
   * 计算每小时的传感器读数统计
   */
  def calculateHourlyStats(dataFrame: DataFrame): DataFrame = {
    // 添加小时列
    val dfWithHour = dataFrame.withColumn(
      "hour", 
      date_format(from_unixtime(col("timestamp") / 1000), "yyyy-MM-dd HH")
    )
    
    // 按小时和传感器分组计算统计
    dfWithHour.groupBy("hour", "sensorId")
      .agg(
        avg("temperature").as("avgTemperature"),
        avg("humidity").as("avgHumidity"),
        avg("soilMoisture").as("avgSoilMoisture"),
        avg("lightIntensity").as("avgLightIntensity"),
        count("*").as("readingCount")
      )
      .orderBy("hour", "sensorId")
  }

  /**
   * 检测异常值（超过阈值的数据）
   */
  def detectAnomalies(dataFrame: DataFrame, tempThreshold: Double, humidityThreshold: Double): DataFrame = {
    dataFrame.filter(
      col("temperature") > tempThreshold || 
      col("temperature") < 0 || 
      col("humidity") > humidityThreshold || 
      col("humidity") < 0 ||
      col("soilMoisture") > 100 ||
      col("soilMoisture") < 0
    ).orderBy(desc("timestamp"))
  }

  /**
   * 保存分析结果到CSV文件
   */
  def saveResultsToCSV(dataFrame: DataFrame, outputPath: String): Unit = {
    dataFrame.write
      .option("header", "true")
      .mode("overwrite")
      .csv(outputPath)
  }
} 