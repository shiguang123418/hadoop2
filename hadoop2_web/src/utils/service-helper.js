/**
 * 服务帮助工具 - 用于处理API路径和服务配置
 */
import apiConfig from '../config/api.config';
import axios from 'axios';

/**
 * 构建API路径
 * @param {string} serviceName 服务名称（如hdfs, hive, kafka等）
 * @param {string} endpoint 端点路径
 * @returns {string} 完整的API路径（包含正确的服务器前缀）
 */
export function buildApiPath(serviceName, endpoint = '') {
  // 获取服务配置
  const serviceConfig = getServiceConfig(serviceName);
  
  // 确保路径格式正确
  const formattedEndpoint = endpoint.startsWith('/') ? endpoint : (endpoint ? '/' + endpoint : '');
  
  // 返回包含服务器前缀和服务路径的完整路径
  return `${serviceConfig.server}${serviceConfig.path}${formattedEndpoint}`;
}

/**
 * 获取服务配置
 * @param {string} serviceName 服务名称（hdfs, hive, auth等）
 * @returns {object} 服务配置对象，包含path和server属性
 */
export function getServiceConfig(serviceName) {
  if (!apiConfig.services[serviceName]) {
    console.warn(`未找到服务配置: ${serviceName}，使用默认配置`);
    return {
      path: `/${serviceName}`,
      server: apiConfig.servers.default
    };
  }
  
  // 返回服务配置
  return apiConfig.services[serviceName];
}

/**
 * 获取服务路径（向后兼容）
 * @param {string} serviceName 服务名称
 * @returns {string} 服务路径
 * @deprecated 请使用getServiceConfig代替
 */
export function getServicePath(serviceName) {
  console.warn('getServicePath方法已废弃，请使用getServiceConfig获取完整服务配置');
  return getServiceConfig(serviceName).path;
}

/**
 * 打印请求路径信息
 * @param {string} method 请求方法
 * @param {string} path 请求路径
 */
export function logRequestPath(method, path) {
  console.log(`${method.toUpperCase()} 请求: ${path}`);
  // 不再使用baseURL，因为完整路径已经包含了服务器前缀
  console.log(`完整URL: ${window.location.origin}${path}`);
}

/**
 * 添加请求拦截器，根据服务名称路由请求到不同的后端服务
 */
export function setupApiInterceptor() {
  axios.interceptors.request.use(
    (config) => {
      const url = config.url || '';
      
      // 已经有完整URL或已经以服务器前缀开头的请求不做处理
      if (url.startsWith('http') || 
          url.startsWith(apiConfig.servers.default) || 
          url.startsWith(apiConfig.servers.server1) || 
          url.startsWith(apiConfig.servers.server2)) {
        return config;
      }
      
      // 尝试识别请求的服务名称，以确定应该使用哪个服务器
      let targetServer = apiConfig.servers.default;
      
      // 提取服务名称（假设URL格式为"/serviceName/...")
      const serviceParts = url.split('/').filter(Boolean);
      if (serviceParts.length > 0) {
        const possibleService = serviceParts[0];
        // 查找服务配置
        if (apiConfig.services[possibleService]) {
          targetServer = apiConfig.services[possibleService].server;
          console.log(`识别到服务 ${possibleService}，使用服务器 ${targetServer}`);
        }
      }
      
      // 使用识别到的服务器前缀
      config.url = targetServer + (url.startsWith('/') ? url : '/' + url);
      console.log('API拦截器处理路径:', url, '->', config.url);
      
      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );
  
  console.log('多后端API请求拦截器已设置，将根据服务自动路由请求');
}

export default {
  buildApiPath,
  getServiceConfig,
  getServicePath, // 保留向后兼容
  logRequestPath,
  setupApiInterceptor
}; 