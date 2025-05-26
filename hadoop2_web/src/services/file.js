import axios from 'axios';
import authService from './auth';
import apiConfig from '../config/api.config';

/**
 * 文件服务
 */
class FileService {
  constructor() {
    // 初始化服务配置
    this.baseUrl = '/api/file';
    console.log('文件服务初始化，baseUrl:', this.baseUrl);
  }
  
  /**
   * 上传文件
   * @param {File} file 文件对象
   * @param {string} dir 可选的目录路径
   * @returns {Promise<Object>} 上传结果
   */
  async uploadFile(file, dir = 'avatar/') {
    try {
      const formData = new FormData();
      formData.append('file', file);
      if (dir) {
        formData.append('dir', dir);
      }
      
      console.log('开始上传文件:', file.name, '目录:', dir);
      
      const response = await axios.post(`${this.baseUrl}/upload`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
          'Authorization': `Bearer ${authService.getToken()}`
        },
        timeout: 60000 // 60秒超时
      });
      
      console.log('文件上传响应:', response.data);
      
      if (response.data && response.data.code === 200) {
        return {
          code: 200,
          message: response.data.message || '上传成功',
          data: response.data.data
        };
      }
      
      return response.data;
    } catch (error) {
      console.error('文件上传失败:', error.response?.data?.message || error.message);
      throw error;
    }
  }
  
  /**
   * 简化版头像上传 - 直接上传不裁剪
   * @param {File} file 头像文件
   * @returns {Promise<Object>} 上传结果
   */
  async uploadAvatarSimple(file) {
    try {
      console.log('开始简化版头像上传:', file.name, '大小:', file.size, '类型:', file.type);
      
      const formData = new FormData();
      formData.append('file', file);
      
      // 使用完整的API路径，确保添加/api前缀
      const response = await axios({
        method: 'post',
        url: '/user/profile/avatar',
        data: formData,
        headers: {
          'Content-Type': 'multipart/form-data',
          'Authorization': `Bearer ${authService.getToken()}`
        },
        timeout: 60000 // 60秒超时
      });
      
      console.log('头像上传响应:', response.data);
      
      if (response.data && response.data.code === 200) {
        // 尝试提取URL
        const result = response.data.data;
        let imageUrl = '';
        
        if (typeof result === 'string') {
          // 直接返回URL字符串
          imageUrl = result;
        } else if (result && typeof result === 'object') {
          // 检查各种可能的字段
          if (result.url) {
            imageUrl = result.url;
          } else if (result.path) {
            imageUrl = result.path;
          } else if (result.filename) {
            // 构建URL
            const datePart = new Date().toISOString().slice(0, 10).replace(/-/g, '/');
            imageUrl = `https://shiguang123418.oss-cn-hangzhou.aliyuncs.com/avatar/${datePart}/${result.filename}`;
          }
        }
        
        if (imageUrl) {
          return {
            code: 200,
            message: '头像上传成功',
            data: imageUrl
          };
        }
        
        // 如果无法提取URL，返回原始响应
        return {
          code: 200,
          message: '头像上传成功，但无法解析URL',
          data: result,
          rawResponse: response.data
        };
      }
      
      return {
        code: response.data.code || 500,
        message: response.data.message || '头像上传失败',
        data: null
      };
    } catch (error) {
      console.error('头像上传失败:', error);
      
      if (error.response) {
        console.error('错误状态码:', error.response.status);
        console.error('错误详情:', error.response.data);
      }
      
      throw error;
    }
  }
  
  /**
   * 上传头像 - 使用专用的头像上传API
   * @param {File} file 头像文件
   * @returns {Promise<Object>} 上传结果
   */
  async uploadAvatar(file) {
    // 直接使用简化版上传
    return this.uploadAvatarSimple(file);
  }
  
  /**
   * 删除文件
   * @param {string} url 文件URL
   * @returns {Promise<Object>} 删除结果
   */
  async deleteFile(url) {
    try {
      const response = await axios.delete(`${this.baseUrl}/delete`, {
        params: { url },
        headers: {
          'Authorization': `Bearer ${authService.getToken()}`
        }
      });
      
      return response.data;
    } catch (error) {
      console.error('文件删除失败:', error.response?.data?.message || error.message);
      throw error;
    }
  }

  /**
   * 更新用户头像 - 将头像URL更新到用户资料
   * @param {string} avatarUrl 头像URL
   * @returns {Promise<Object>} 更新结果
   */
  async updateUserAvatar(avatarUrl) {
    try {
      console.log('更新用户资料中的头像URL:', avatarUrl);
      
      // 调用authService的updateProfile方法，只传递avatar字段
      const response = await authService.updateProfile({ avatar: avatarUrl });
      
      console.log('头像URL更新响应:', response);
      
      return response;
    } catch (error) {
      console.error('更新用户头像失败:', error.response?.data?.message || error.message);
      throw error;
    }
  }
}

export default new FileService(); 