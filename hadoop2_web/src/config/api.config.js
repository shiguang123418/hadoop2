/**
 * API配置文件
 * 包含API地址和相关设置
 */

const config = {
  // API基础路径 (在开发环境下通过代理转发到实际后端地址)
  baseURL: '/api',
  
  // API超时设置（毫秒）
  timeout: 15000,
  
  // 是否启用调试日志
  debug: true,
  
  // Kafka相关配置
  kafka: {
    // 默认订阅的主题
    defaultTopics: ['agriculture-sensor-data', 'agriculture-weather-data'],
    
    // 数据轮询间隔（毫秒）
    pollingInterval: 5000,
    
    // 当API请求失败时是否使用模拟数据
    useMockOnFailure: true
  },
  
  // 服务器配置
  servers: {
    main: 'http://192.168.1.192:8080',
    local: 'http://localhost:8080'
  },
  
  // 是否直接连接后端（绕过代理）
  // 设置为false时，将通过Vite代理访问后端
  directConnection: false,
  
};

export default config; 