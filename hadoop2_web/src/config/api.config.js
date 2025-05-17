/**
 * API配置文件
 * 包含API地址和相关设置
 */

const config = {
  // API基础路径
  baseURL: '/api',
  
  // API超时设置（毫秒）
  timeout: 10000,
  
  // 是否启用调试日志
  debug: true,
  
  // Kafka相关配置
  kafka: {
    // 默认订阅的主题
    defaultTopics: ['agriculture-sensor-data'],
    
    // 数据轮询间隔（毫秒）
    pollingInterval: 3000,
    
    // 当API请求失败时是否使用模拟数据
    useMockOnFailure: true
  },
  
  // 后端服务器地址
  serverUrl: 'http://192.168.1.192:8080'
};

export default config; 