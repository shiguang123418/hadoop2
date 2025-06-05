/**
 * WebSocket配置文件
 * 与API配置保持一致，方便统一管理连接地址
 */

import apiConfig from './api.config';

// 配置WebSocket连接
const config = {
  // WebSocket端点路径
  // 使用统一的代理路径
  get wsUrl() {
    // 对所有环境都使用统一代理地址
    return '/api_ws';
  },
  
  // 是否使用SockJS
  useSockJS: true,
  
  // 自动重连配置
  reconnectDelay: 5000,
  maxReconnectAttempts: 10,
  
  // STOMP主题
  topics: {
    weatherStats: '/topic/weather-stats',
    productStats: '/topic/product-stats',
    generalUpdates: '/topic/updates',
    agricultureSensorData: '/topic/agriculture-sensor-data',
    sparkStats: '/topic/spark-stats'
  }
};

export default config; 