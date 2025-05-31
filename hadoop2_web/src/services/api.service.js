import axios from 'axios';
import apiConfig from '../config/api.config';
import { buildApiPath, getServiceConfig } from '../utils/service-helper';

/**
 * API服务基类 - 提供统一的API调用方法
 */
class ApiService {
  /**
   * 构造函数，设置API路径
   * @param {string} serviceName 服务名称，如 'hdfs', 'hive', 'kafka' 等
   */
  constructor(serviceName) {
    // 获取服务配置
    const serviceConfig = getServiceConfig(serviceName);
    
    // 保存服务信息
    this.serviceName = serviceName;
    this.servicePath = serviceConfig.path;
    this.serverPrefix = serviceConfig.server;
    
    // 创建axios实例，baseURL为空字符串，让全局axios配置处理
    this.api = axios.create({
      baseURL: '',
      headers: {
        'Content-Type': 'application/json',
      }
    });
    
    console.log(`初始化服务 ${serviceName}, 路径: ${this.serverPrefix}${this.servicePath}`);
    
    // 请求拦截器
    this.api.interceptors.request.use(
      config => {
        // 设置认证头
        const token = localStorage.getItem('token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        
        // 记录请求URL信息
        const fullUrl = (config.baseURL || '') + config.url;
        console.log(`发送API请求: ${config.method.toUpperCase()} ${fullUrl}`);
        
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
    console.log(`GET请求: ${url}`);
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
    console.log(`POST请求: ${url}`);
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
    console.log(`PUT请求: ${url}`);
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
    console.log(`DELETE请求: ${url}`);
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
    
    // 返回完整路径
    return `${this.serverPrefix}${this.servicePath}${endpoint}`;
  }
}

export default ApiService; 