import axios from 'axios';
import authService from './auth';
import apiConfig from '../config/api.config';

/**
 * 文件服务
 */
class FileService {
  constructor() {
    // file服务不是标准服务，需要单独设置路径
    // axios.defaults.baseURL 已经设置为 /api，这里不要重复添加
    this.baseUrl = '/file';
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
        }
      });
      
      console.log('文件上传响应:', response.data);
      
      // 规范化返回结果
      if (response.data) {
        if (response.data.code === 200) {
          // 检查data字段的格式
          if (response.data.data) {
            // 确保返回标准格式
            return {
              code: 200,
              message: response.data.message || '上传成功',
              data: response.data.data
            };
          }
        }
      }
      
      return response.data;
    } catch (error) {
      console.error('文件上传失败:', error.response?.data?.message || error.message);
      throw error;
    }
  }
  
  /**
   * 上传头像 - 使用专用的头像上传API
   * @param {File} file 头像文件
   * @returns {Promise<Object>} 上传结果
   */
  async uploadAvatar(file) {
    try {
      const formData = new FormData();
      formData.append('file', file);
      
      console.log('开始上传头像:', file.name);
      
      // 使用专门的头像上传API
      // axios.defaults.baseURL 已经设置为 /api，这里使用不带/api前缀的路径
      const userProfileUrl = '/user/profile/avatar';
      console.log('头像上传URL:', userProfileUrl, '完整URL:', axios.defaults.baseURL + userProfileUrl);
      
      const response = await axios.post(userProfileUrl, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
          'Authorization': `Bearer ${authService.getToken()}`
        }
      });
      
      console.log('头像上传响应:', response.data);
      
      return response.data;
    } catch (error) {
      console.error('头像上传失败:', error.response?.data?.message || error.message);
      throw error;
    }
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