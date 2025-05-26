/**
 * 服务帮助工具 - 用于处理API路径和服务配置
 */
import apiConfig from '../config/api.config';
import axios from 'axios';

/**
 * 构建API路径
 * @param {string} servicePath 服务路径（不含/api前缀）
 * @param {string} endpoint 端点路径
 * @returns {string} 完整的API路径
 */
export function buildApiPath(servicePath, endpoint = '') {
  // 确保路径格式正确
  const formattedServicePath = servicePath.startsWith('/') ? servicePath : '/' + servicePath;
  const formattedEndpoint = endpoint.startsWith('/') ? endpoint : (endpoint ? '/' + endpoint : '');
  
  // 不再手动添加/api前缀，让拦截器统一处理
  return `${formattedServicePath}${formattedEndpoint}`;
}

/**
 * 获取服务路径
 * @param {string} serviceName 服务名称（hdfs, hive, auth等）
 * @returns {string} 服务的API路径（不含/api前缀）
 */
export function getServicePath(serviceName) {
  if (!apiConfig.services[serviceName]) {
    console.warn(`未找到服务配置: ${serviceName}`);
    return `/${serviceName}`;
  }
  
  // 返回不含/api前缀的服务路径
  return apiConfig.services[serviceName];
}

/**
 * 打印请求路径信息
 * @param {string} method 请求方法
 * @param {string} path 请求路径
 */
export function logRequestPath(method, path) {
  console.log(`${method.toUpperCase()} 请求: ${path}`);
  console.log(`完整URL: ${axios.defaults.baseURL}${path.startsWith('/') ? path : '/' + path}`);
}

/**
 * 添加请求拦截器，确保所有API请求都带有/api前缀
 */
export function setupApiInterceptor() {
  axios.interceptors.request.use(
    (config) => {
      const url = config.url || '';
      
      // 已经有完整URL或已经有/api前缀的请求不做处理
      if (url.startsWith('http') || url.startsWith('/api')) {
        return config;
      }
      
      // 所有非完整URL的请求都添加/api前缀
      config.url = '/api' + (url.startsWith('/') ? url : '/' + url);
      console.log('API拦截器处理路径:', url, '->', config.url);
      
      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );
  
  console.log('API请求拦截器已设置，将自动为所有请求添加/api前缀');
}

export default {
  buildApiPath,
  getServicePath,
  logRequestPath,
  setupApiInterceptor
}; 