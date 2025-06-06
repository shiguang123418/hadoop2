import axios from 'axios'
import ApiService from '../services/api.service'
import logger from '../utils/logger'

/**
 * 传感器数据Hive分析API服务
 * 封装传感器数据Hive分析相关的API请求
 */
class SensorHiveApiService extends ApiService {
  constructor() {
    // 使用服务名称
    super('hive');
    logger.debug('传感器数据Hive分析服务初始化');
  }

  /**
   * 获取传感器数据表信息
   * @returns {Promise} API响应
   */
  getSensorTableInfo() {
    logger.debug('API: 获取传感器数据表信息');
    return this.get('/sensor/info');
  }

  /**
   * 获取传感器类型列表
   * @returns {Promise} API响应
   */
  getSensorTypes() {
    logger.debug('API: 获取传感器类型列表');
    return this.get('/sensor/types');
  }

  /**
   * 获取传感器位置列表
   * @returns {Promise} API响应
   */
  getSensorLocations() {
    logger.debug('API: 获取传感器位置列表');
    return this.get('/sensor/locations');
  }

  /**
   * 获取传感器数据统计信息
   * @param {Object} params 查询参数
   * @returns {Promise} API响应
   */
  getSensorStats(params) {
    logger.debug('API: 获取传感器数据统计信息', params);
    return this.get('/sensor/stats', params);
  }

  /**
   * 获取传感器时间序列数据
   * @param {Object} params 查询参数
   * @returns {Promise} API响应
   */
  getTimeSeries(params) {
    logger.debug('API: 获取传感器时间序列数据', params);
    return this.get('/sensor/time-series', params);
  }

  /**
   * 获取传感器相关性数据
   * @param {Object} params 查询参数
   * @returns {Promise} API响应
   */
  getCorrelation(params) {
    logger.debug('API: 获取传感器相关性数据', params);
    return this.get('/sensor/correlation', params);
  }

  /**
   * 触发传感器数据导入
   * @returns {Promise} API响应
   */
  triggerDataImport() {
    logger.debug('API: 触发传感器数据导入');
    return this.post('/sensor/import');
  }
}

// 创建单例
const SensorHiveApi = new SensorHiveApiService();

// 导出服务单例
export default SensorHiveApi;

// 命名导出，用于从index.js中导入
export { SensorHiveApi }; 