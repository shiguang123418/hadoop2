/**
 * API配置文件
 * 统一管理API路径配置
 */

// API基础路径 - 保留为空，由拦截器统一处理
const baseUrl = '';

// 服务相关配置，不包含/api前缀
const services = {
  hdfs: '/hdfs',
  hive: '/hive',
  spark: '/spark',
  kafka: '/kafka',
  auth: '/auth',
  realtime: '/realtime'
};

// 导出配置
export default {
  // 基础URL，用于全局axios配置
  baseUrl,
  // 服务路径配置
  services
}; 