/**
 * 错误处理工具
 * 提供统一的错误处理机制
 */

import logger from './logger';
import { ElMessage } from 'element-plus';

/**
 * HTTP状态码错误映射
 */
const HTTP_ERROR_MESSAGES = {
  400: '请求参数错误',
  401: '未授权，请重新登录',
  403: '拒绝访问',
  404: '请求的资源不存在',
  405: '不支持的请求方法',
  408: '请求超时',
  409: '资源冲突',
  410: '资源已删除',
  429: '请求次数过多，请稍后重试',
  500: '服务器内部错误',
  501: '服务未实现',
  502: '网关错误',
  503: '服务不可用',
  504: '网关超时',
};

/**
 * 处理API错误
 * @param {Error} error 错误对象
 * @param {Object} options 配置选项
 * @param {boolean} options.showMessage 是否显示消息提示
 * @param {boolean} options.throwError 是否继续抛出错误
 * @returns {Error} 原始错误
 */
export const handleApiError = (error, options = { showMessage: true, throwError: true }) => {
  // 提取错误信息
  let errorMessage = '请求失败，请稍后重试';
  let errorDetails = '';
  
  // 记录错误
  if (error.response) {
    // 服务器返回错误状态码
    const statusCode = error.response.status;
    errorMessage = HTTP_ERROR_MESSAGES[statusCode] || `请求失败(${statusCode})`;
    
    // 尝试从响应中获取更详细的错误信息
    if (error.response.data) {
      if (typeof error.response.data === 'string') {
        errorDetails = error.response.data;
      } else if (error.response.data.message) {
        errorDetails = error.response.data.message;
      } else if (error.response.data.error) {
        errorDetails = error.response.data.error;
      }
    }

    logger.error(`API错误 [${statusCode}]: ${errorMessage}`, {
      url: error.config?.url,
      method: error.config?.method,
      details: errorDetails || '无详细信息',
      response: error.response.data
    });
  } else if (error.request) {
    // 请求已发送但没有收到响应
    errorMessage = '网络错误，未收到服务器响应';
    logger.error('网络错误: 没有收到响应', {
      url: error.config?.url,
      method: error.config?.method,
      error: error.message
    });
  } else {
    // 请求设置时发生错误
    errorMessage = '请求配置错误';
    logger.error('请求错误', {
      message: error.message,
      error
    });
  }

  // 显示错误消息
  if (options.showMessage) {
    const showMessage = errorDetails 
      ? `${errorMessage}: ${errorDetails}`
      : errorMessage;
    
    ElMessage({
      message: showMessage,
      type: 'error',
      duration: 5000,
      showClose: true
    });
  }

  // 重新抛出错误或返回
  if (options.throwError) {
    throw error;
  }
  
  return error;
};

/**
 * 创建异步错误处理包装器
 * @param {Function} asyncFunction 异步函数
 * @param {Object} options 错误处理选项
 * @returns {Function} 包装后的函数
 */
export const withErrorHandling = (asyncFunction, options = { showMessage: true, throwError: false }) => {
  return async (...args) => {
    try {
      return await asyncFunction(...args);
    } catch (error) {
      return handleApiError(error, options);
    }
  };
};

export default {
  handleApiError,
  withErrorHandling
}; 