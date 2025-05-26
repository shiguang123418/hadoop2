import axios from 'axios';
import apiConfig from '../config/api.config';

/**
 * 身份验证服务
 */
class AuthService {
  constructor() {
    // 构建基础URL，使用apiConfig中配置的服务路径
    // 注意：services.auth 不包含 /api 前缀，axios.defaults.baseURL 会负责添加
    this.baseUrl = apiConfig.services.auth;
    console.log('Auth服务初始化，baseUrl:', this.baseUrl, '完整URL:', axios.defaults.baseURL + this.baseUrl);
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
    return this.hasRole('admin');
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

      console.log(`发送登录请求到: ${this.baseUrl}/login`);
      
      // 使用相对路径，让代理处理路由
      const loginUrl = `${this.baseUrl}/login`;
      console.log(`实际请求URL: ${loginUrl}，完整URL: ${axios.defaults.baseURL}${loginUrl}`);
      
      const response = await axios.post(loginUrl, { username, password }, {
        withCredentials: true,
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        // 添加超时设置
        timeout: 10000
      });
      
      console.log('登录响应数据:', response.data);
      
      // 检查响应中是否包含token
      if (response.data && response.data.token) {
        console.log('保存登录凭证到本地存储');
        localStorage.setItem('token', response.data.token);
        
        // 保存用户信息
        const userData = response.data.user || { username: username, role: 'user' };
        
        // 打印用户数据，检查是否包含avatar
        console.log('保存用户数据:', userData);
        if (userData.avatar) {
          console.log('用户头像URL:', userData.avatar);
        }
        
        localStorage.setItem('user', JSON.stringify(userData));
        
        // 设置默认Authorization头
        axios.defaults.headers.common['Authorization'] = `Bearer ${response.data.token}`;
        
        console.log('登录成功，已设置认证头');
      } else if (response.data && response.data.data && response.data.data.token) {
        // 处理嵌套在data字段中的情况
        console.log('从嵌套的data字段中提取登录凭证');
        const token = response.data.data.token;
        const userData = response.data.data.user || { username: username, role: 'user' };
        
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
        console.warn('响应中未找到token', response.data);
        throw new Error('服务器返回数据格式不正确，未找到令牌信息');
      }
      
      return response.data;
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
      
      // 创建一个副本，只包含需要更新的字段
      const profileToUpdate = {...profileData};
      
      // 确保不发送角色信息，避免角色格式问题
      if (profileToUpdate.role) delete profileToUpdate.role;
      if (profileToUpdate.roles) delete profileToUpdate.roles;
      
      console.log('发送个人资料更新请求，数据:', profileToUpdate);
      
      const response = await axios.put(`${this.baseUrl}/profile`, profileToUpdate, {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      });
      
      // 更新本地存储的用户信息
      const updatedUser = {
        ...currentUser,
        ...profileToUpdate
      };
      
      // 确保avatar字段正确保存
      if (profileToUpdate.avatar) {
        updatedUser.avatar = profileToUpdate.avatar;
      }
      
      localStorage.setItem('user', JSON.stringify(updatedUser));
      
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