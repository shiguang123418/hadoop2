import axios from 'axios';
import AuthService from './auth';
import apiConfig from '../config/api.config';

/**
 * API服务基类 - 提供统一的API调用方法
 */
class ApiService {
  /**
   * 构造函数，设置API路径
   * @param {string} basePath API基础路径
   */
  constructor(basePath) {
    // 确保basePath有正确的格式
    if (basePath.startsWith('/')) {
      this.servicePath = basePath;
    } else {
      this.servicePath = '/' + basePath;
    }
    
    // 创建axios实例，baseURL为空字符串，让全局axios配置处理
    this.api = axios.create({
      baseURL: '',
      headers: {
        'Content-Type': 'application/json',
      }
    });
    
    console.log(`初始化服务 ${basePath}, 全局axios baseURL: ${axios.defaults.baseURL}, 完整路径前缀: ${axios.defaults.baseURL}${this.servicePath}`);
    
    // 请求拦截器
    this.api.interceptors.request.use(
      config => {
        // 设置认证头
        const token = AuthService.getToken();
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      error => Promise.reject(error)
    );
    
    // 响应拦截器
    this.api.interceptors.response.use(
      response => {
        // 直接返回整个响应数据，不做过滤处理
        // 在具体服务中处理数据结构
        return response.data;
      },
      error => {
        // 处理错误响应
        console.error('API请求失败:', error);
        return Promise.reject(error);
      }
    );
  }
  
  /**
   * 发送GET请求
   * @param {string} endpoint API端点
   * @param {Object} params URL参数
   * @param {Object} config 请求配置
   * @returns {Promise} 响应结果
   */
  get(endpoint, params = {}, config = {}) {
    const url = this.buildUrl(endpoint);
    console.log(`GET请求: ${axios.defaults.baseURL}${url}`);
    return this.api.get(url, {
      params,
      ...config
    });
  }
  
  /**
   * 发送POST请求
   * @param {string} endpoint API端点
   * @param {Object} data 请求体数据
   * @param {Object} config 请求配置
   * @returns {Promise} 响应结果
   */
  post(endpoint, data = {}, config = {}) {
    const url = this.buildUrl(endpoint);
    console.log(`POST请求: ${axios.defaults.baseURL}${url}`);
    return this.api.post(url, data, config);
  }
  
  /**
   * 发送PUT请求
   * @param {string} endpoint API端点
   * @param {Object} data 请求体数据
   * @param {Object} config 请求配置
   * @returns {Promise} 响应结果
   */
  put(endpoint, data = {}, config = {}) {
    const url = this.buildUrl(endpoint);
    console.log(`PUT请求: ${axios.defaults.baseURL}${url}`);
    return this.api.put(url, data, config);
  }
  
  /**
   * 发送DELETE请求
   * @param {string} endpoint API端点
   * @param {Object} params URL参数
   * @param {Object} config 请求配置
   * @returns {Promise} 响应结果
   */
  delete(endpoint, params = {}, config = {}) {
    const url = this.buildUrl(endpoint);
    console.log(`DELETE请求: ${axios.defaults.baseURL}${url}`);
    return this.api.delete(url, {
      params,
      ...config
    });
  }
  
  /**
   * 构建完整的URL
   * @param {string} endpoint API端点
   * @returns {string} 完整URL
   */
  buildUrl(endpoint) {
    // 确保endpoint格式正确
    endpoint = endpoint.startsWith('/') ? endpoint : '/' + endpoint;
    
    // 返回服务路径和端点的组合
    return `${this.servicePath}${endpoint}`;
  }
}

export default ApiService; 