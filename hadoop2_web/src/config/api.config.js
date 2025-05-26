/**
 * API配置文件
 * 统一管理API路径配置
 */

// API基础路径
const baseUrl = '/api';

// 服务相关配置，不包含/api前缀，避免重复
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
  // 代理模式设置（保留，但不再影响路径构建）
  useProxy: true,
  // 导出服务路径配置
  services
}; 