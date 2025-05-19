import axios from 'axios';
import apiConfig from '@/config/api.config';

/**
 * API服务基类 - 提供所有服务共享的基础功能
 */
class ApiService {
  /**
   * @param {string} baseEndpoint 服务的API端点 (如 '/hdfs', '/hive')
   */
  constructor(baseEndpoint) {
    // 配置API基础路径
    if (apiConfig.directConnection && apiConfig.servers && apiConfig.servers.main) {
      // 直连模式：使用服务器URL
      const serverUrl = apiConfig.servers.main;
      this.baseUrl = `${serverUrl}/api${baseEndpoint}`;
      console.log(`使用直连模式，API基础路径: ${this.baseUrl}`);
    } else {
      // 代理模式：使用相对路径
      this.baseUrl = `${apiConfig.baseURL || '/api'}${baseEndpoint}`;
      console.log(`使用代理模式，API基础路径: ${this.baseUrl}`);
    }
    
    // 是否启用调试
    this.debug = apiConfig.debug;
    
    // 配置默认请求超时和重试
    this.axios = axios.create({
      timeout: apiConfig.timeout,
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    // 添加请求拦截器
    this.axios.interceptors.request.use(config => {
      if (this.debug) {
        console.log(`API请求 → ${config.method.toUpperCase()} ${config.url}`, 
                    config.params || (config.data && JSON.stringify(config.data).length < 1000 ? config.data : '[大数据]'));
      }
      return config;
    });
    
    // 添加响应拦截器
    this.axios.interceptors.response.use(
      response => {
        if (this.debug) {
          console.log(`API响应 ← ${response.status}`, 
                      response.data ? (typeof response.data === 'object' ? '[响应数据]' : response.data) : '[无数据]');
        }
        return response;
      },
      error => {
        console.error('API错误:', error.response ? `${error.response.status} ${error.response.statusText}` : error.message);
        return Promise.reject(error);
      }
    );
  }
  
  /**
   * 执行GET请求
   * @param {string} endpoint API端点
   * @param {Object} params 请求参数
   * @param {Object} config 额外配置
   * @returns {Promise} 响应Promise
   */
  get(endpoint, params = {}, config = {}) {
    return this.axios.get(`${this.baseUrl}${endpoint}`, { 
      ...config,
      params 
    });
  }
  
  /**
   * 执行POST请求
   * @param {string} endpoint API端点
   * @param {Object} data 请求体数据
   * @param {Object} config 额外配置
   * @returns {Promise} 响应Promise
   */
  post(endpoint, data = {}, config = {}) {
    return this.axios.post(`${this.baseUrl}${endpoint}`, data, config);
  }
  
  /**
   * 执行PUT请求
   * @param {string} endpoint API端点
   * @param {Object} data 请求体数据
   * @param {Object} config 额外配置
   * @returns {Promise} 响应Promise
   */
  put(endpoint, data = {}, config = {}) {
    return this.axios.put(`${this.baseUrl}${endpoint}`, data, config);
  }
  
  /**
   * 执行DELETE请求
   * @param {string} endpoint API端点
   * @param {Object} params 请求参数
   * @param {Object} config 额外配置
   * @returns {Promise} 响应Promise
   */
  delete(endpoint, params = {}, config = {}) {
    return this.axios.delete(`${this.baseUrl}${endpoint}`, { 
      ...config,
      params 
    });
  }
  
  /**
   * 切换到备用服务器
   * @param {string} serverType 服务器类型 ('main', 'local', 等)
   * @returns {boolean} 是否切换成功
   */
  switchServer(serverType = 'main') {
    if (!apiConfig.servers || !apiConfig.servers[serverType]) {
      console.error(`未知服务器类型: ${serverType}`);
      return false;
    }
    
    if (apiConfig.directConnection) {
      const serverUrl = apiConfig.servers[serverType];
      const endpoint = this.baseUrl.split('/api')[1];
      this.baseUrl = `${serverUrl}/api${endpoint}`;
      console.log(`已切换到服务器 ${serverType}: ${this.baseUrl}`);
      return true;
    } else {
      console.log('代理模式下，无需切换服务器');
      return false;
    }
  }
}

export default ApiService; 