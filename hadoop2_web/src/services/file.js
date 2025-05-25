import axios from 'axios';
import authService from './auth';

/**
 * 文件服务
 */
class FileService {
  constructor() {
    this.baseUrl = '/file';
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
      const response = await axios.post('/user/profile/avatar', formData, {
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
}

export default new FileService(); 