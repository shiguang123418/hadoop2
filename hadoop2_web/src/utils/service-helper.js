/**
 * 服务帮助工具 - 用于处理API路径和服务配置
 */
import apiConfig from '../config/api.config';
import axios from 'axios';
import logger from './logger';
// 导入路由，用于在token过期时重定向
import router from '../router';
// 移除循环引用，改为动态处理AuthService
// import AuthService from '../services/auth';
// 导入Element Plus的消息组件
import { ElMessage } from 'element-plus';

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
    logger.warn(`未找到服务配置: ${serviceName}，使用默认配置`);
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
  logger.warn('getServicePath方法已废弃，请使用getServiceConfig获取完整服务配置');
  return getServiceConfig(serviceName).path;
}

/**
 * 打印请求路径信息
 * @param {string} method 请求方法
 * @param {string} path 请求路径
 */
export function logRequestPath(method, path) {
  logger.debug(`${method.toUpperCase()} 请求: ${path}`);
  // 不再使用baseURL，因为完整路径已经包含了服务器前缀
  logger.debug(`完整URL: ${window.location.origin}${path}`);
}

/**
 * 执行登出操作
 * 移除localStorage中的token和user信息
 */
function logout() {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  delete axios.defaults.headers.common['Authorization'];
  logger.info('用户已登出');
}

/**
 * 添加请求拦截器，根据服务名称路由请求到不同的后端服务
 */
export function setupApiInterceptor() {
  // 请求拦截器 - 路由不同服务到不同后端服务器
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
          logger.debug(`识别到服务 ${possibleService}，使用服务器 ${targetServer}`);
        }
      }
      
      // 使用识别到的服务器前缀
      config.url = targetServer + (url.startsWith('/') ? url : '/' + url);
      logger.debug('API拦截器处理路径:', url, '->', config.url);
      
      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );

  // 响应拦截器 - 处理认证错误（token过期）
  axios.interceptors.response.use(
    (response) => {
      return response;
    },
    (error) => {
      if (error.response) {
        // 处理401错误（未授权）- 通常表示token已过期或无效
        if (error.response.status === 401) {
          logger.warn('收到401未授权响应，可能是令牌过期');
          
          // 检查当前路由是否需要认证
          const currentPath = router.currentRoute.value.path;
          const needsAuth = router.currentRoute.value.meta.requiresAuth;
          
          // 执行登出操作 - 不再直接调用AuthService
          logout();
          
          // 如果当前页面需要认证，则重定向到登录页面
          if (needsAuth && currentPath !== '/login') {
            logger.info('重定向到登录页面，当前路径:', currentPath);
            router.push('/login');
            
            // 显示消息提示用户
            ElMessage.warning({
              message: '您的登录已过期，请重新登录',
              duration: 5000
            });
          }
        }
      }
      
      // 继续抛出错误，让调用代码处理
      return Promise.reject(error);
    }
  );
  
  logger.info('多后端API请求拦截器和响应拦截器已设置，将自动处理令牌过期情况');
}

export default {
  buildApiPath,
  getServiceConfig,
  getServicePath, // 保留向后兼容
  logRequestPath,
  setupApiInterceptor
}; 