/**
 * API配置文件
 * 统一管理API路径配置
 */

// API基础路径 - 保留为空，由拦截器统一处理
const baseUrl = '';

// 多后端服务器配置
const servers = {
  // 默认API服务器(原有API)
  default: '/api1',
  // 服务器1 - HDFS, Hive相关服务
  server1: '/api1',
  // 服务器2 - Kafka, Spark, Realtime相关服务
  server2: '/api2'
};

// 服务相关配置
const services = {
  // HDFS服务 - 使用服务器1
  hdfs: {
    path: '/hdfs',
    server: servers.server1
  },
  // Hive服务 - 使用服务器1
  hive: {
    path: '/hive',
    server: servers.server1
  },
  // Spark服务 - 使用服务器2
  spark: {
    path: '/spark',
    server: servers.server1
  },
  // Kafka服务 - 使用服务器2
  kafka: {
    path: '/kafka',
    server: servers.server1
  },
  // 认证服务 - 使用默认服务器
  auth: {
    path: '/auth',
    server: servers.default
  },
  // 实时数据服务 - 使用服务器2
  realtime: {
    path: '/realtime',
    server: servers.server2
  }
};

// 导出配置
export default {
  // 基础URL，用于全局axios配置
  baseUrl,
  // 服务路径配置
  services,
  // 服务器配置
  servers
}; 