import axios from 'axios'

/**
 * 传感器API服务
 * 封装传感器相关的API请求
 */
export default {
  /**
   * 发送测试数据
   * @returns {Promise} API响应
   */
  sendTestData() {
    console.log('API: 发送测试数据请求');
    return axios.get('/api/sensor/direct-test', {
      timeout: 10000, // 添加10秒超时
      headers: {
        'Cache-Control': 'no-cache',
        'Pragma': 'no-cache'
      }
    }).catch(error => {
      console.error('API: 发送测试数据请求失败', error);
      // 重新抛出错误，让调用者处理
      throw error;
    });
  },

  /**
   * 获取系统状态
   * @returns {Promise} API响应
   */
  getSystemStatus() {
    console.log('API: 获取系统状态');
    return axios.get('/api/system/status', {
      timeout: 5000 // 添加5秒超时
    }).catch(error => {
      console.error('API: 获取系统状态失败', error);
      throw error;
    });
  },

  /**
   * 测试WebSocket连接
   * @returns {Promise} API响应
   */
  testWebSocket() {
    console.log('API: 测试WebSocket连接');
    return axios.get('/api/system/test-websocket', {
      timeout: 5000 // 添加5秒超时
    }).catch(error => {
      console.error('API: 测试WebSocket连接失败', error);
      throw error;
    });
  },

  /**
   * 获取传感器历史数据
   * @param {Object} params 查询参数
   * @param {String} params.sensorId 传感器ID
   * @param {String} params.startTime 开始时间
   * @param {String} params.endTime 结束时间
   * @returns {Promise} API响应
   */
  getSensorHistory(params) {
    console.log('API: 获取传感器历史数据', params);
    return axios.get('/api/sensor/history', { 
      params,
      timeout: 10000 // 添加10秒超时
    }).catch(error => {
      console.error('API: 获取传感器历史数据失败', error);
      throw error;
    });
  },

  /**
   * 获取异常数据
   * @param {Object} params 查询参数
   * @returns {Promise} API响应
   */
  getAnomalyData(params) {
    console.log('API: 获取异常数据', params);
    return axios.get('/api/sensor/anomalies', { 
      params,
      timeout: 10000 // 添加10秒超时
    }).catch(error => {
      console.error('API: 获取异常数据失败', error);
      throw error;
    });
  }
} 