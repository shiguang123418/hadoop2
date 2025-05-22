package org.shiguang.utils

import com.typesafe.config.{Config, ConfigFactory}
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import java.io.File
import scala.util.{Try, Success, Failure}
import java.nio.file.{Paths, Files}

/**
 * 配置管理类，提供统一的配置访问和管理功能
 */
object ConfigManager {
  private var configInstance: Option[Config] = None
  
  // 配置文件名
  private val CONFIG_FILENAME = "application.conf"
  
  // 环境变量配置
  private val ENV_CONFIG_PATH = "CONFIG_PATH"
  private val ENV_ACTIVE_PROFILE = "ACTIVE_PROFILE"
  
  /**
   * 加载配置文件
   * 优先级：
   * 1. 指定的环境变量路径
   * 2. 指定的系统属性路径
   * 3. 项目资源目录下的配置
   */
  def loadConfig(): Config = {
    if (configInstance.isEmpty) {
      val config = loadConfigFromSources()
      configInstance = Some(config)
      
      // 打印配置信息（仅用于调试）
      println(s"配置加载完成，当前激活的环境: ${getActiveProfile}")
    }
    
    configInstance.get
  }
  
  /**
   * 从各种来源加载配置
   */
  private def loadConfigFromSources(): Config = {
    // 1. 检查环境变量中指定的配置文件
    val envConfigPath = Option(System.getenv(ENV_CONFIG_PATH))
    val envConfig = envConfigPath.flatMap { path =>
      loadConfigFromFile(new File(path))
    }
    
    // 2. 从类路径加载配置
    val classPathConfig = ConfigFactory.load(CONFIG_FILENAME)
    
    // 3. 合并配置（外部配置优先级高于内部配置）
    val combinedConfig = envConfig match {
      case Some(extConfig) => extConfig.withFallback(classPathConfig)
      case None => classPathConfig
    }
    
    // 4. 替换占位符并应用环境特定的配置
    val resolvedConfig = applyProfileSpecificConfig(combinedConfig)
    
    resolvedConfig
  }
  
  /**
   * 从文件加载配置
   */
  private def loadConfigFromFile(file: File): Option[Config] = {
    if (file.exists() && file.isFile) {
      try {
        Some(ConfigFactory.parseFile(file))
      } catch {
        case e: Exception =>
          println(s"从文件加载配置失败: ${file.getAbsolutePath}, 错误: ${e.getMessage}")
          None
      }
    } else {
      println(s"配置文件不存在: ${file.getAbsolutePath}")
      None
    }
  }
  
  /**
   * 应用环境特定的配置
   */
  private def applyProfileSpecificConfig(baseConfig: Config): Config = {
    val profile = getActiveProfile
    val profileConfig = s"profiles.$profile"
    
    if (baseConfig.hasPath(profileConfig)) {
      // 如果存在特定环境的配置，将其与基础配置合并
      val envSpecificConfig = baseConfig.getConfig(profileConfig)
      envSpecificConfig.withFallback(baseConfig).resolve()
    } else {
      baseConfig.resolve()
    }
  }
  
  /**
   * 获取当前激活的环境
   */
  def getActiveProfile: String = {
    Option(System.getenv(ENV_ACTIVE_PROFILE))
      .orElse(Option(System.getProperty(ENV_ACTIVE_PROFILE)))
      .getOrElse("dev") // 默认为开发环境
  }
  
  /**
   * 获取字符串配置项
   */
  def getString(path: String, default: String = ""): String = {
    val config = loadConfig()
    if (config.hasPath(path)) config.getString(path) else default
  }
  
  /**
   * 获取整数配置项
   */
  def getInt(path: String, default: Int = 0): Int = {
    val config = loadConfig()
    if (config.hasPath(path)) config.getInt(path) else default
  }
  
  /**
   * 获取长整型配置项
   */
  def getLong(path: String, default: Long = 0): Long = {
    val config = loadConfig()
    if (config.hasPath(path)) config.getLong(path) else default
  }
  
  /**
   * 获取布尔型配置项
   */
  def getBoolean(path: String, default: Boolean = false): Boolean = {
    val config = loadConfig()
    if (config.hasPath(path)) config.getBoolean(path) else default
  }
  
  /**
   * 获取双精度浮点型配置项
   */
  def getDouble(path: String, default: Double = 0.0): Double = {
    val config = loadConfig()
    if (config.hasPath(path)) config.getDouble(path) else default
  }
  
  /**
   * 获取持续时间配置项（例如 5s, 10m 等）
   */
  def getDuration(path: String, default: Duration = 0.seconds): Duration = {
    val config = loadConfig()
    if (config.hasPath(path)) {
      config.getDuration(path, java.util.concurrent.TimeUnit.MILLISECONDS).millis
    } else {
      default
    }
  }
  
  /**
   * 获取字符串列表配置项
   */
  def getStringList(path: String): List[String] = {
    val config = loadConfig()
    if (config.hasPath(path)) {
      config.getStringList(path).asScala.toList
    } else {
      List.empty[String]
    }
  }
  
  /**
   * 获取子配置
   */
  def getConfig(path: String): Option[Config] = {
    val config = loadConfig()
    if (config.hasPath(path)) Some(config.getConfig(path)) else None
  }
} 