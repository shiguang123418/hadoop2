import axios from 'axios';
import apiConfig from '../config/api.config';

/**
 * 身份验证服务
 */
class AuthService {
  constructor() {
    // 不要在 baseUrl 中添加 /api 前缀，因为 axios.defaults.baseURL 已经是 /api 了
    // 这里只使用相对路径 /auth
    this.baseUrl = '/auth';
    console.log('Auth服务初始化，baseUrl:', this.baseUrl);
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
      console.error('解析用户信息失败:', e);
      localStorage.removeItem('user');
      return null;
    }
  }
  
  /**
   * 获取身份验证令牌
   */
  getToken() {
    const token = localStorage.getItem('token');
    console.log('获取Token:', token ? '存在' : '不存在');
    return token;
  }
  
  /**
   * 检查用户是否已登录
   */
  isLoggedIn() {
    const isLoggedIn = !!this.getToken();
    console.log('检查登录状态:', isLoggedIn);
    return isLoggedIn;
  }
  
  /**
   * 检查用户是否为管理员
   */
  isAdmin() {
    const currentUser = this.getCurrentUser();
    
    // 检查用户名是否为admin
    if (currentUser && currentUser.username === 'admin') {
      return true;
    }
    
    // 检查roles数组
    if (currentUser && currentUser.roles && Array.isArray(currentUser.roles)) {
      return currentUser.roles.some(role => 
        role === 'admin' || role === 'ROLE_ADMIN'
      );
    }
    
    // 检查role字段（兼容旧结构）
    return currentUser && (currentUser.role === 'admin' || currentUser.role === 'ROLE_ADMIN');
  }
  
  /**
   * 检查用户是否具有指定角色
   * @param {string} role 角色名称
   * @returns {boolean} 是否具有该角色
   */
  hasRole(role) {
    const currentUser = this.getCurrentUser();
    if (!currentUser) return false;
    
    // 检查roles数组
    if (currentUser.roles && Array.isArray(currentUser.roles)) {
      return currentUser.roles.some(userRole => 
        userRole === role || 
        userRole === `ROLE_${role}` || 
        role === `ROLE_${userRole}`
      );
    }
    
    // 检查role字段（兼容旧结构）
    return currentUser.role === role || 
           currentUser.role === `ROLE_${role}` || 
           role === `ROLE_${currentUser.role}`;
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
      
      console.log('登录响应数据:', response.data);
      
      // 检查响应中是否包含token
      if (response.data && response.data.token) {
        console.log('保存登录凭证到本地存储');
        localStorage.setItem('token', response.data.token);
        
        // 保存用户信息
        const userData = response.data.user || { username: username, role: 'user' };
        localStorage.setItem('user', JSON.stringify(userData));
        
        // 设置默认Authorization头
        axios.defaults.headers.common['Authorization'] = `Bearer ${response.data.token}`;
        
        console.log('登录成功，已设置认证头');
      } else if (response.data && response.data.data && response.data.data.token) {
        // 处理嵌套在data字段中的情况
        console.log('从嵌套的data字段中提取登录凭证');
        const token = response.data.data.token;
        const userData = response.data.data.user || { username: username, role: 'user' };
        
        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(userData));
        
        // 设置默认Authorization头
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        
        console.log('登录成功，已设置认证头');
      } else {
        console.warn('响应中未找到token', response.data);
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
  
  /**
   * 获取用户列表
   * 需要管理员权限
   */
  async getUsers() {
    try {
      const response = await axios.get(`${this.baseUrl}/users`, {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      });
      return response.data;
    } catch (error) {
      console.error('获取用户列表失败:', error.response?.data?.message || error.message);
      throw error;
    }
  }
  
  /**
   * 获取单个用户信息
   * @param {string} userId 用户ID
   */
  async getUser(userId) {
    try {
      const response = await axios.get(`${this.baseUrl}/users/${userId}`, {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      });
      return response.data;
    } catch (error) {
      console.error('获取用户信息失败:', error.response?.data?.message || error.message);
      throw error;
    }
  }
  
  /**
   * 创建用户
   * 需要管理员权限
   * @param {Object} userData 用户数据
   */
  async createUser(userData) {
    try {
      const response = await axios.post(`${this.baseUrl}/users`, userData, {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      });
      return response.data;
    } catch (error) {
      console.error('创建用户失败:', error.response?.data?.message || error.message);
      throw error;
    }
  }
  
  /**
   * 更新用户
   * 需要管理员权限或者是用户本人
   * @param {string} userId 用户ID
   * @param {Object} userData 用户数据
   */
  async updateUser(userId, userData) {
    try {
      const response = await axios.put(`${this.baseUrl}/users/${userId}`, userData, {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      });
      
      // 如果更新的是当前用户，同步更新本地存储
      const currentUser = this.getCurrentUser();
      if (currentUser && currentUser.id === userId) {
        localStorage.setItem('user', JSON.stringify({
          ...currentUser,
          ...userData
        }));
      }
      
      return response.data;
    } catch (error) {
      console.error('更新用户失败:', error.response?.data?.message || error.message);
      throw error;
    }
  }
  
  /**
   * 删除用户
   * 需要管理员权限
   * @param {string} userId 用户ID
   */
  async deleteUser(userId) {
    try {
      const response = await axios.delete(`${this.baseUrl}/users/${userId}`, {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      });
      return response.data;
    } catch (error) {
      console.error('删除用户失败:', error.response?.data?.message || error.message);
      throw error;
    }
  }
  
  /**
   * 更改用户密码
   * @param {string} currentPassword 当前密码
   * @param {string} newPassword 新密码
   */
  async changePassword(currentPassword, newPassword) {
    try {
      const response = await axios.post(`${this.baseUrl}/change-password`, {
        currentPassword,
        newPassword
      }, {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      });
      return response.data;
    } catch (error) {
      console.error('更改密码失败:', error.response?.data?.message || error.message);
      throw error;
    }
  }
  
  /**
   * 更新当前用户资料
   * @param {Object} profileData 用户资料数据
   */
  async updateProfile(profileData) {
    try {
      const currentUser = this.getCurrentUser();
      if (!currentUser) {
        throw new Error('未登录');
      }
      
      const response = await axios.put(`${this.baseUrl}/profile`, profileData, {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      });
      
      // 更新本地存储的用户信息
      localStorage.setItem('user', JSON.stringify({
        ...currentUser,
        ...profileData
      }));
      
      return response.data;
    } catch (error) {
      console.error('更新用户资料失败:', error.response?.data?.message || error.message);
      throw error;
    }
  }
  
  /**
   * 管理员重置用户密码
   * 需要管理员权限
   * @param {string} userId 用户ID
   */
  async resetUserPassword(userId) {
    try {
      const response = await axios.post(`${this.baseUrl}/users/${userId}/reset-password`, {}, {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      });
      return response.data;
    } catch (error) {
      console.error('重置用户密码失败:', error.response?.data?.message || error.message);
      throw error;
    }
  }
}

export default new AuthService(); 