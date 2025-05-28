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
    return axios.get('/api/sensor/direct-test')
  },

  /**
   * 获取系统状态
   * @returns {Promise} API响应
   */
  getSystemStatus() {
    return axios.get('/api/system/status')
  },

  /**
   * 测试WebSocket连接
   * @returns {Promise} API响应
   */
  testWebSocket() {
    return axios.get('/api/system/test-websocket')
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
    return axios.get('/api/sensor/history', { params })
  },

  /**
   * 获取异常数据
   * @param {Object} params 查询参数
   * @returns {Promise} API响应
   */
  getAnomalyData(params) {
    return axios.get('/api/sensor/anomalies', { params })
  }
} 