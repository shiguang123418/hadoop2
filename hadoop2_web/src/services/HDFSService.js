import ApiService from './api.service';

/**
 * HDFS服务 - 提供与HDFS文件系统交互的功能
 */
class HDFSService extends ApiService {
  constructor() {
    super('/hdfs');
  }
  
  /**
   * 获取HDFS连接状态
   * @returns {Promise} 连接状态信息
   */
  getStatus() {
    return this.get('/status');
  }
  
  /**
   * 列出目录内容
   * @param {string} path 目录路径
   * @returns {Promise} 文件和目录列表
   */
  listFiles(path) {
    return this.get('/list', { path });
  }
  
  /**
   * 创建目录
   * @param {string} path 目录路径
   * @param {string} permission 可选，权限字符串(如"755")
   * @returns {Promise} 创建结果
   */
  createDirectory(path, permission) {
    // 确保路径开头有斜杠
    if (!path.startsWith('/')) {
      path = '/' + path;
    }
    
    // 创建请求数据对象
    const requestData = { path };
    if (permission) {
      requestData.permission = permission;
    }
    
    // 使用请求体而不是URL参数发送路径信息
    return this.post('/mkdir', requestData);
  }
  
  /**
   * 上传文件
   * @param {File} file 文件对象
   * @param {string} targetPath 目标路径
   * @param {Function} progressCallback 进度回调函数
   * @returns {Promise} 上传结果
   */
  uploadFile(file, targetPath, progressCallback) {
    const formData = new FormData();
    formData.append('file', file);
    
    const config = {
      params: { path: targetPath },
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    };
    
    // 如果提供了进度回调，添加上传进度监听
    if (typeof progressCallback === 'function') {
      config.onUploadProgress = progressEvent => {
        const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        progressCallback(percentCompleted, progressEvent);
      };
    }
    
    return this.post('/upload', formData, config);
  }
  
  /**
   * 下载文件
   * @param {string} path 文件路径
   * @param {Function} progressCallback 进度回调函数
   * @returns {Promise} 文件内容
   */
  downloadFile(path, progressCallback) {
    const config = {
      responseType: 'blob'
    };
    
    // 如果提供了进度回调，添加下载进度监听
    if (typeof progressCallback === 'function') {
      config.onDownloadProgress = progressEvent => {
        const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        progressCallback(percentCompleted, progressEvent);
      };
    }
    
    return this.get('/download', { path }, config);
  }
  
  /**
   * 删除文件或目录
   * @param {string} path 路径
   * @param {boolean} recursive 是否递归删除
   * @returns {Promise} 删除结果
   */
  deleteFile(path, recursive = false) {
    return this.delete('/delete', { path, recursive });
  }
  
  /**
   * 移动或重命名文件
   * @param {string} source 源路径
   * @param {string} destination 目标路径
   * @returns {Promise} 操作结果
   */
  renameFile(source, destination) {
    return this.post('/rename', null, {
      params: { src: source, dst: destination }
    });
  }
  
  /**
   * 检查文件是否存在
   * @param {string} path 路径
   * @returns {Promise} 存在检查结果
   */
  fileExists(path) {
    return this.get('/exists', { path });
  }
  
  /**
   * 保存下载的文件到本地
   * @param {Blob} blob 文件Blob对象
   * @param {string} filename 文件名
   */
  saveFile(blob, filename) {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    
    // 清理
    window.URL.revokeObjectURL(url);
    document.body.removeChild(link);
  }
}

// 导出单例
export default new HDFSService(); 