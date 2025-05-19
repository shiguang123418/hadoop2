import axios from 'axios';
import apiConfig from '@/config/api.config';

/**
 * 身份验证服务
 */
class AuthService {
  constructor() {
    // 配置API基础路径
    if (apiConfig.directConnection && apiConfig.servers && apiConfig.servers.main) {
      // 直连模式：使用服务器URL
      const serverUrl = apiConfig.servers.main;
      this.baseUrl = `${serverUrl}/api/auth`;
    } else {
      // 代理模式：使用相对路径
      this.baseUrl = `${apiConfig.baseURL || '/api'}/auth`;
    }
  }
  
  /**
   * 获取当前登录用户
   */
  getCurrentUser() {
    const userStr = localStorage.getItem('user');
    if (!userStr) return null;
    
    try {
      return JSON.parse(userStr);
    } catch (e) {
      localStorage.removeItem('user');
      return null;
    }
  }
  
  /**
   * 获取身份验证令牌
   */
  getToken() {
    return localStorage.getItem('token');
  }
  
  /**
   * 检查用户是否已登录
   */
  isLoggedIn() {
    return !!this.getToken();
  }
  
  /**
   * 登录
   * @param {string} username 用户名
   * @param {string} password 密码
   */
  async login(username, password) {
    try {
      console.log('AuthService: 尝试登录', { username });
      
      // 使用正确的登录端点
      const response = await axios.post(`${this.baseUrl}/login`, { username, password }, {
        withCredentials: true,
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        }
      });
      
      console.log('AuthService: 登录响应', response.data);
      
      if (response.data.token) {
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('user', JSON.stringify(response.data.user));
        
        // 设置默认Authorization头
        axios.defaults.headers.common['Authorization'] = `Bearer ${response.data.token}`;
      }
      return response.data;
    } catch (error) {
      console.error('AuthService: 登录失败', error);
      throw error;
    }
  }
  
  /**
   * 注册
   * @param {Object} userData 用户数据
   */
  async register(userData) {
    try {
      const response = await axios.post(`${this.baseUrl}/register`, userData);
      return response.data;
    } catch (error) {
      console.error('AuthService: 注册失败', error);
      throw error;
    }
  }
  
  /**
   * 注销
   */
  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    axios.defaults.headers.common['Authorization'] = '';
  }
  
  /**
   * 设置身份验证头
   */
  setupAuthHeader() {
    const token = this.getToken();
    if (token) {
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    } else {
      delete axios.defaults.headers.common['Authorization'];
    }
  }
}

export default new AuthService(); 