/**
 * 日志工具
 * 提供统一的日志记录接口，可根据环境配置不同的日志行为
 */
import axios from 'axios';

// 日志级别定义
const LOG_LEVELS = {
  DEBUG: 0,
  INFO: 1,
  WARN: 2,
  ERROR: 3,
  NONE: 4
};

// 日志级别名称映射
const LOG_LEVEL_NAMES = {
  0: 'DEBUG',
  1: 'INFO',
  2: 'WARN',
  3: 'ERROR',
  4: 'NONE'
};

// 当前环境的日志级别
const CURRENT_LOG_LEVEL = process.env.NODE_ENV === 'production' 
  ? LOG_LEVELS.ERROR  // 生产环境只显示错误
  : LOG_LEVELS.DEBUG; // 开发环境显示所有日志

// 是否启用远程日志收集（仅在生产环境中）
const ENABLE_REMOTE_LOGGING = process.env.NODE_ENV === 'production';


// 最大缓存日志数量（批量发送）
const MAX_CACHED_LOGS = 10;

/**
 * 日志记录器
 */
class Logger {
  constructor() {
    this.level = CURRENT_LOG_LEVEL;
    this.cachedLogs = [];
    this.remoteLoggingEnabled = ENABLE_REMOTE_LOGGING;
    this.applicationName = 'hadoop2_web';
    this.userInfo = null;
  }

  /**
   * 设置日志级别
   * @param {number} level 日志级别
   */
  setLevel(level) {
    if (Object.values(LOG_LEVELS).includes(level)) {
      this.level = level;
    }
  }

  /**
   * 记录调试日志
   * @param {string} message 日志消息
   * @param {any} data 附加数据
   */
  debug(message, data) {
    this._log(LOG_LEVELS.DEBUG, message, data);
  }

  /**
   * 记录信息日志
   * @param {string} message 日志消息
   * @param {any} data 附加数据
   */
  info(message, data) {
    this._log(LOG_LEVELS.INFO, message, data);
  }

  /**
   * 记录警告日志
   * @param {string} message 日志消息
   * @param {any} data 附加数据
   */
  warn(message, data) {
    this._log(LOG_LEVELS.WARN, message, data);
  }

  /**
   * 记录错误日志
   * @param {string} message 日志消息
   * @param {any} error 错误对象
   */
  error(message, error) {
    this._log(LOG_LEVELS.ERROR, message, error);
  }

  /**
   * 内部日志记录方法
   * @param {number} level 日志级别
   * @param {string} message 日志消息
   * @param {any} data 附加数据
   * @private
   */
  _log(level, message, data) {
    if (level < this.level) {
      return;
    }

    const timestamp = new Date().toISOString();
    const levelName = LOG_LEVEL_NAMES[level];
    
    // 控制台输出
    if (data) {
      switch (level) {
        case LOG_LEVELS.DEBUG:
          console.debug(`[${levelName}] ${message}`, data);
          break;
        case LOG_LEVELS.INFO:
          console.info(`[${levelName}] ${message}`, data);
          break;
        case LOG_LEVELS.WARN:
          console.warn(`[${levelName}] ${message}`, data);
          break;
        case LOG_LEVELS.ERROR:
          console.error(`[${levelName}] ${message}`, data);
          break;
      }
    } else {
      switch (level) {
        case LOG_LEVELS.DEBUG:
          console.debug(`[${levelName}] ${message}`);
          break;
        case LOG_LEVELS.INFO:
          console.info(`[${levelName}] ${message}`);
          break;
        case LOG_LEVELS.WARN:
          console.warn(`[${levelName}] ${message}`);
          break;
        case LOG_LEVELS.ERROR:
          console.error(`[${levelName}] ${message}`);
          break;
      }
    }

    // 远程日志记录（只记录WARN及以上级别的日志）
    if (this.remoteLoggingEnabled && level >= LOG_LEVELS.WARN) {
      this._addToRemoteLogQueue({
        level: levelName,
        message,
        data: this._sanitizeData(data),
        timestamp,
        application: this.applicationName,
        userInfo: this.userInfo,
        url: window.location.href,
        userAgent: navigator.userAgent
      });
    }
  }

  /**
   * 清理数据以便安全发送到服务器
   * @param {any} data 需要清理的数据
   * @returns {any} 清理后的数据
   * @private
   */
  _sanitizeData(data) {
    if (!data) return null;

    // 如果是错误对象，提取关键信息
    if (data instanceof Error) {
      return {
        name: data.name,
        message: data.message,
        stack: data.stack
      };
    }

    // 如果是DOM元素或复杂对象，转换为安全的字符串表示
    try {
      // 先尝试用JSON.stringify处理，如果失败则转换为字符串
      const serialized = JSON.stringify(data);
      return JSON.parse(serialized);
    } catch (e) {
      return String(data);
    }
  }

  /**
   * 添加日志到远程队列
   * @param {Object} logEntry 日志条目
   * @private
   */
  _addToRemoteLogQueue(logEntry) {
    this.cachedLogs.push(logEntry);
  
  }

  /**
   * 立即发送所有缓存的日志
   */
  flush() {

  }
}

// 创建单例
const logger = new Logger();

// 页面卸载前尝试发送所有日志
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', () => {
    logger.flush();
  });
}

// 导出日志记录器
export default logger; 