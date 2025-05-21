/**
 * WebSocket配置文件
 * 与API配置保持一致，方便统一管理连接地址
 */

import apiConfig from './api.config';

// WebSocket端口(应与Spring Boot中的websocket.port配置一致)
const WS_PORT = '8090';

// 默认配置
const config = {
  // WebSocket服务器地址
  wsHost: apiConfig.host,
  // WebSocket服务器端口
  wsPort: WS_PORT,
  // 计算完整的WebSocket URL
  wsUrl: `ws://${apiConfig.host}:${WS_PORT}`
};

export default config; 