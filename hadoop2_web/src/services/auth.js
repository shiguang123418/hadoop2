import axios from 'axios';
import apiConfig from '../config/api.config';
import ApiService from './api.service';
import { getServiceConfig } from '../utils/service-helper';

/**
 * 身份验证服务
 */
class AuthServiceClass extends ApiService {
  constructor() {
    // 使用服务名称
    super('auth');
    console.log('Auth服务初始化');
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
    // 使用统一的角色检查，检查是否有 admin 或 ROLE_ADMIN 角色
    return this.hasRole('role_admin');
  }
  
  /**
   * 检查用户是否具有指定角色
   * @param {string} role 角色名称
   * @returns {boolean} 是否具有该角色
   */
  hasRole(role) {
    const currentUser = this.getCurrentUser();
    if (!currentUser) return false;
    
    // 如果提供的角色为空，返回false
    if (!role) return false;
    
    // 标准化角色名称（处理ROLE_前缀）
    const normalizeRoleName = (roleName) => {
      if (!roleName || typeof roleName !== 'string') return '';
      
      const lowerRole = roleName.toLowerCase();
      // 如果已有前缀，直接返回；否则加上前缀
      return lowerRole.startsWith('role_') ? lowerRole : 'role_' + lowerRole;
    };
    
    const normalizedRoleToCheck = normalizeRoleName(role);
    console.log('检查角色:', role, '标准化后:', normalizedRoleToCheck);
    
    // 检查roles数组
    if (currentUser.roles && Array.isArray(currentUser.roles)) {
      return currentUser.roles.some(userRole => {
        const normalizedUserRole = normalizeRoleName(userRole);
        console.log('比较用户角色:', userRole, '标准化后:', normalizedUserRole);
        return normalizedUserRole === normalizedRoleToCheck;
      });
    }
    
    // 检查role字段（兼容旧结构）
    if (!currentUser.role) return false;
    
    const normalizedUserRole = normalizeRoleName(currentUser.role);
    console.log('比较单一角色:', currentUser.role, '标准化后:', normalizedUserRole);
    return normalizedUserRole === normalizedRoleToCheck;
  }
  
  /**
   * 登录
   * @param {string} username 用户名
   * @param {string} password 密码
   */
  async login(username, password) {
    try {
      // 检查用户名和密码
      if (!username || !password) {
        throw new Error('用户名和密码不能为空');
      }

      console.log(`发送登录请求`);
      
      // 使用API服务类提供的post方法
      const response = await this.post('/login', { username, password });
      
      console.log('登录响应数据:', response);
      
      // 检查响应中是否包含token
      if (response && response.token) {
        console.log('保存登录凭证到本地存储');
        localStorage.setItem('token', response.token);
        
        // 保存用户信息
        const userData = response.user || { username: username, role: 'user' };
        
        // 打印用户数据，检查是否包含avatar
        console.log('保存用户数据:', userData);
        if (userData.avatar) {
          console.log('用户头像URL:', userData.avatar);
        }
        
        localStorage.setItem('user', JSON.stringify(userData));
        
        // 设置默认Authorization头
        axios.defaults.headers.common['Authorization'] = `Bearer ${response.token}`;
        
        console.log('登录成功，已设置认证头');
      } else if (response && response.data && response.data.token) {
        // 处理嵌套在data字段中的情况
        console.log('从嵌套的data字段中提取登录凭证');
        const token = response.data.token;
        const userData = response.data.user || { username: username, role: 'user' };
        
        // 打印用户数据，检查是否包含avatar
        console.log('保存用户数据(嵌套):', userData);
        if (userData.avatar) {
          console.log('用户头像URL:', userData.avatar);
        }
        
        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(userData));
        
        // 设置默认Authorization头
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        
        console.log('登录成功，已设置认证头');
      } else {
        console.warn('响应中未找到token', response);
        throw new Error('服务器返回数据格式不正确，未找到令牌信息');
      }
      
      return response;
    } catch (error) {
      console.error('登录失败:', error);
      
      // 详细记录错误信息用于调试
      if (error.response) {
        console.error('错误状态码:', error.response.status);
        console.error('错误信息:', error.response.data);
        
        // 针对常见HTTP状态码提供更友好的错误消息
        if (error.response.status === 401) {
          throw new Error('用户名或密码错误');
        } else if (error.response.status === 403) {
          throw new Error('账号已被锁定或禁用');
        } else if (error.response.status >= 500) {
          throw new Error('服务器内部错误，请联系管理员');
        }
      } else if (error.request) {
        // 请求已发出但没有收到响应
        console.error('未收到服务器响应:', error.request);
        throw new Error('无法连接到服务器，请检查网络连接');
      }
      
      // 如果上面的条件都不符合，则抛出原始错误
      throw error;
    }
  }
  
  /**
   * 注册新用户
   * @param {Object} userData 用户数据
   */
  async register(userData) {
    // 使用API服务类提供的post方法
    return this.post('/register', userData);
  }
  
  /**
   * 退出登录
   */
  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    delete axios.defaults.headers.common['Authorization'];
  }
  
  /**
   * 设置认证头
   */
  setupAuthHeader() {
    const token = this.getToken();
    if (token) {
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
      return true;
    }
    return false;
  }
  
  /**
   * 获取所有用户（管理员功能）
   */
  async getUsers() {
    return this.get('/users');
  }
  
  /**
   * 获取特定用户信息
   * @param {string} userId 用户ID
   */
  async getUser(userId) {
    return this.get(`/users/${userId}`);
  }
  
  /**
   * 创建新用户（管理员功能）
   * @param {Object} userData 用户数据
   */
  async createUser(userData) {
    return this.post('/users', userData);
  }
  
  /**
   * 更新用户信息（管理员功能）
   * @param {string} userId 用户ID
   * @param {Object} userData 用户数据
   */
  async updateUser(userId, userData) {
    return this.put(`/users/${userId}`, userData);
  }
  
  /**
   * 删除用户（管理员功能）
   * @param {string} userId 用户ID
   */
  async deleteUser(userId) {
    return this.delete(`/users/${userId}`);
  }
  
  /**
   * 修改当前用户密码
   * @param {string} currentPassword 当前密码
   * @param {string} newPassword 新密码
   */
  async changePassword(currentPassword, newPassword) {
    return this.post('/change-password', {
      currentPassword,
      newPassword
    });
  }
  
  /**
   * 更新当前用户个人资料
   * @param {Object} profileData 个人资料数据
   */
  async updateProfile(profileData) {
    return this.put('/profile', profileData);
  }
  
  /**
   * 重置用户密码（管理员功能）
   * @param {string} userId 用户ID
   */
  async resetUserPassword(userId) {
    return this.post(`/users/${userId}/reset-password`);
  }
}

// 创建单例
const AuthService = new AuthServiceClass();

// 导出服务单例
export default AuthService;

// 命名导出，用于从index.js中导入
export { AuthService }; 