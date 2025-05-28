/**
 * WebSocket配置文件
 * 与API配置保持一致，方便统一管理连接地址
 */

import apiConfig from './api.config';

// 配置WebSocket连接
const config = {
  // WebSocket服务器地址 - 使用与API相同的服务器
  wsHost: window.location.hostname || 'localhost',
  
  // WebSocket服务端点 - 需与后端Spring配置一致
  wsEndpoint: '/api/ws',
  
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
    notifications: '/topic/notifications'
  },
  
  // 计算完整的WebSocket URL
  get wsUrl() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = this.wsHost;
    const port = window.location.port ? `:${window.location.port}` : '';
    const endpoint = this.wsEndpoint;
    
    // 如果使用SockJS，添加sockjs后缀
    const sockjsSuffix = this.useSockJS ? '' : '';
    
    return `${protocol}//${host}${port}${endpoint}${sockjsSuffix}`;
  }
};

export default config; 