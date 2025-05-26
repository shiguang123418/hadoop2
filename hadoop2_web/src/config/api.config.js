/**
 * API配置文件
 * 集中管理所有API地址配置，方便统一修改IP地址和端口
 */

// ======================================================
// 【重要】修改此处可统一更改所有API服务地址
// ======================================================
const API_SERVER_HOST = 'shiguang'; // 修改为localhost或实际IP，不要使用主机名
const API_SERVER_PORT = '8000';      // 修改此处设置API服务器端口
// ======================================================

// 默认开发环境配置
const devConfig = {
  // API服务器地址
  apiServer: `http://${API_SERVER_HOST}:${API_SERVER_PORT}`,
  // API基础路径
  apiBasePath: '/api',
  // 是否启用本地代理（开发时使用）
  useProxy: false, // 直接使用完整URL，不使用代理
  // 服务相关配置
  services: {
    hdfs: '/hdfs',
    hive: '/hive',
    spark: '/spark',
    kafka: '/kafka',
    auth: '/auth',
    realtime: '/realtime'
  }
};

// 生产环境配置
const prodConfig = {
  // API服务器地址，生产环境可指定实际服务器地址
  apiServer: `http://${API_SERVER_HOST}:${API_SERVER_PORT}`,
  // API基础路径
  apiBasePath: '/api',
  // 生产环境不使用代理
  useProxy: false,
  // 服务相关配置（与开发环境相同）
  services: { ...devConfig.services }
};

// 根据环境选择配置
const isProduction = import.meta.env.PROD;
const config = isProduction ? prodConfig : devConfig;

// 计算完整的API基础URL
const baseUrl = config.apiServer + config.apiBasePath;

// 导出配置
export default {
  baseUrl,
  services: {
    hdfs: '/hdfs',
    hive: '/hive',
    spark: '/spark',
    kafka: '/kafka', 
    auth: '/auth',
    realtime: '/realtime'
  }
}; 