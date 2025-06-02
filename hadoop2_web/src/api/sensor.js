import axios from 'axios'
import ApiService from '../services/api.service'
import { buildApiPath } from '../utils/service-helper'
import logger from '../utils/logger'

/**
 * 传感器API服务
 * 封装传感器相关的API请求
 */
class SensorApiService extends ApiService {
  constructor() {
    // 使用服务名称
    super('realtime');
    logger.debug('传感器服务初始化');
  }

  /**
   * 获取系统状态
   * @returns {Promise} API响应
   */
  getSystemStatus() {
    logger.debug('API: 获取系统状态');
    return this.get('/system/status');
  }

  /**
   * 测试WebSocket连接
   * @returns {Promise} API响应
   */
  testWebSocket() {
    logger.debug('API: 测试WebSocket连接');
    return this.get('/system/test-websocket');
  }

  /**
   * 获取传感器历史数据
   * @param {Object} params 查询参数
   * @param {String} params.sensorId 传感器ID
   * @param {String} params.startTime 开始时间
   * @param {String} params.endTime 结束时间
   * @returns {Promise} API响应
   */
  getSensorHistory(params) {
    logger.debug('API: 获取传感器历史数据', params);
    return this.get('/sensor/history', params);
  }

  /**
   * 获取异常数据
   * @param {Object} params 查询参数
   * @returns {Promise} API响应
   */
  getAnomalyData(params) {
    logger.debug('API: 获取异常数据', params);
    return this.get('/sensor/anomalies', params);
  }
}

// 创建单例
const SensorApi = new SensorApiService();

// 导出服务单例
export default SensorApi;

// 命名导出，用于从index.js中导入
export { SensorApi }; 