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
    this.basePath = basePath;
    this.api = axios.create({
      baseURL: apiConfig.baseUrl,
      headers: {
        'Content-Type': 'application/json',
      }
    });
    
    // 请求拦截器
    this.api.interceptors.request.use(
      config => {
        // 可以在发送请求前做一些处理，例如添加token
        // const token = localStorage.getItem('token');
        // if (token) {
        //   config.headers.Authorization = `Bearer ${token}`;
        // }
        
        // 移除路径中的重复 /api 前缀，因为 baseURL 已经包含了 /api
        if (this.basePath.startsWith('/api')) {
          this.basePath = this.basePath.substring(4);
        }
        
        return config;
      },
      error => Promise.reject(error)
    );
    
    // 响应拦截器
    this.api.interceptors.response.use(
      response => response.data,
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
    return this.api.get(`${this.basePath}${endpoint}`, {
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
    return this.api.post(`${this.basePath}${endpoint}`, data, config);
  }
  
  /**
   * 发送PUT请求
   * @param {string} endpoint API端点
   * @param {Object} data 请求体数据
   * @param {Object} config 请求配置
   * @returns {Promise} 响应结果
   */
  put(endpoint, data = {}, config = {}) {
    return this.api.put(`${this.basePath}${endpoint}`, data, config);
  }
  
  /**
   * 发送DELETE请求
   * @param {string} endpoint API端点
   * @param {Object} params URL参数
   * @param {Object} config 请求配置
   * @returns {Promise} 响应结果
   */
  delete(endpoint, params = {}, config = {}) {
    return this.api.delete(`${this.basePath}${endpoint}`, {
      params,
      ...config
    });
  }
}

export default ApiService; 