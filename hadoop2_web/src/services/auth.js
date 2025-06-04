import axios from 'axios';
import ApiService from './api.service';
import router from '../router';
import { ElMessage } from 'element-plus';

/**
 * 身份验证服务
 */
class AuthServiceClass extends ApiService {
  constructor() {
    // 使用服务名称
    super('auth');
    // console.log('Auth服务初始化');
    
    // 自动启动token有效性检查
    this.startTokenValidityCheck();
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
      // console.error('解析用户信息失败:', e);
      localStorage.removeItem('user');
      return null;
    }
  }
  
  /**
   * 获取身份验证令牌
   */
  getToken() {
    const token = localStorage.getItem('token');
    // console.log('获取Token:', token ? '存在' : '不存在');
    return token;
  }
  
  /**
   * 检查用户是否已登录
   */
  isLoggedIn() {
    const isLoggedIn = !!this.getToken();
    // console.log('检查登录状态:', isLoggedIn);
    return isLoggedIn;
  }
  
  /**
   * 检查用户是否为管理员
   */
  isAdmin() {
    // 使用统一的角色检查，检查是否有 admin 或 ROLE_ADMIN 角色
    return this.hasRole('ROLE_ADMIN');
  }
  
  /**
   * 检查用户是否具有指定角色
   * @param {string} role 角色名称
   * @returns {boolean} 是否具有该角色
   */
  hasRole(role) {
    const currentUser = this.getCurrentUser();
    // logger.info("shigaung",currentUser)
    if (!currentUser) return false;
    
    // 如果提供的角色为空，返回false
    if (!role) return false;

    // 检查role字段（兼容旧结构）
    if (!currentUser.role) return false;

    // 获取当前用户角色和需要检查的角色，去掉可能的ROLE_前缀以进行标准化比较
    const userRole = currentUser.role.replace(/^ROLE_/, '').toLowerCase();
    const checkRole = role.replace(/^ROLE_/, '').toLowerCase();

    // 比较标准化后的角色
    return userRole === checkRole;
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

      // console.log(`发送登录请求`);
      
      // 使用API服务类提供的post方法
      const response = await this.post('/login', { username, password });
      
      // console.log('登录响应数据:', response);
      
      // 检查响应中是否包含token
      if (response && response.token) {
        // console.log('保存登录凭证到本地存储');
        localStorage.setItem('token', response.token);
        
        // 保存用户信息
        const userData = response.user || { username: username, role: 'user' };
        
        // 打印用户数据，检查是否包含avatar
        // console.log('保存用户数据:', userData);
        
        localStorage.setItem('user', JSON.stringify(userData));
        
        // 设置默认Authorization头
        axios.defaults.headers.common['Authorization'] = `Bearer ${response.token}`;
        
        // console.log('登录成功，已设置认证头');
        
        // 启动token有效性检查
        this.startTokenValidityCheck();
      } else if (response && response.data && response.data.token) {
        // 处理嵌套在data字段中的情况
        // console.log('从嵌套的data字段中提取登录凭证');
        const token = response.data.token;
        const userData = response.data.user || { username: username, role: 'user' };
        
        // 打印用户数据，检查是否包含avatar
        // console.log('保存用户数据(嵌套):', userData);
        
        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(userData));
        
        // 设置默认Authorization头
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        
        // console.log('登录成功，已设置认证头');
        
        // 启动token有效性检查
        this.startTokenValidityCheck();
      } else {
        // console.warn('响应中未找到token', response);
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
    
    // 清除token检查定时器
    if (this.tokenCheckInterval) {
      clearInterval(this.tokenCheckInterval);
      this.tokenCheckInterval = null;
    }
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
   * 检查JWT令牌是否有效
   */
  isTokenValid() {
    const token = this.getToken();
    if (!token) return false;
    
    try {
      // 解析JWT token（不需要验证签名）
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
      }).join(''));
      
      const payload = JSON.parse(jsonPayload);
      
      // 检查是否过期
      if (payload.exp) {
        const current = Math.floor(Date.now() / 1000);
        return payload.exp > current;
      }
      
      return true;
    } catch (e) {
      console.error('解析JWT token失败:', e);
      return false;
    }
  }
  
  /**
   * 开始定期检查token有效性
   */
  startTokenValidityCheck() {
    // 清除现有的检查器
    if (this.tokenCheckInterval) {
      clearInterval(this.tokenCheckInterval);
    }
    
    // 每分钟检查一次token有效性
    this.tokenCheckInterval = setInterval(() => {
      if (this.isLoggedIn() && !this.isTokenValid()) {
        console.warn('Token已过期，执行自动登出');
        
        // 如果不在登录页，显示提示
        if (router.currentRoute.value.path !== '/login') {
          ElMessage.error('登录已过期，请重新登录');
        }
        
        // 执行登出
        this.logout();
        
        // 重定向到登录页
        router.push('/login');
      }
    }, 60000); // 每60秒检查一次
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