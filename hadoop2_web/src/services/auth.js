import axios from 'axios';

/**
 * 身份验证服务
 */
class AuthService {
  constructor() {
    // 使用全局配置的API URL
    const baseURL = window.API_BASE_URL || 'http://localhost:8080/api';
    this.baseUrl = `${baseURL}/auth`;
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
      console.log(`发送登录请求到: ${this.baseUrl}/login`);
      const response = await axios.post(`${this.baseUrl}/login`, { username, password }, {
        withCredentials: true,
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        }
      });
      
      if (response.data.token) {
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('user', JSON.stringify(response.data.user));
        
        // 设置默认Authorization头
        axios.defaults.headers.common['Authorization'] = `Bearer ${response.data.token}`;
      }
      return response.data;
    } catch (error) {
      console.error('登录失败:', error.response?.data?.message || error.message);
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
      console.error('注册失败:', error.response?.data?.message || error.message);
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