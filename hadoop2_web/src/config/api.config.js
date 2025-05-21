/**
 * API配置文件
 * 集中管理所有API地址配置，方便统一修改IP地址和端口
 */

// ======================================================
// 【重要】修改此处可统一更改所有API服务地址
// ======================================================
const API_SERVER_HOST = '192.168.110.32'; // 修改此处设置API服务器主机名/IP
const API_SERVER_PORT = '8080';      // 修改此处设置API服务器端口
// ======================================================

// 默认开发环境配置
const devConfig = {
  // API服务器地址
  apiServer: `http://${API_SERVER_HOST}:${API_SERVER_PORT}`,
  // API基础路径
  apiBasePath: '/api',
  // 是否启用本地代理（开发时使用）
  useProxy: true,
  // 服务相关配置
  services: {
    hdfs: '/hdfs',
    hive: '/hive',
    spark: '/spark',
    auth: '/auth'
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
const getBaseUrl = () => {
  // 如果是开发环境且使用代理，则使用相对路径
  if (!isProduction && config.useProxy) {
    return config.apiBasePath;
  }
  // 否则使用完整服务器地址
  return `${config.apiServer}${config.apiBasePath}`;
};

// 导出配置
export default {
  ...config,
  baseUrl: getBaseUrl(),
  // 导出主机和端口以便其他地方使用
  host: API_SERVER_HOST,
  port: API_SERVER_PORT
}; 