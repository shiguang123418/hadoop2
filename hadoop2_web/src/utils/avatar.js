/**
 * 头像URL处理工具
 * 解决头像URL格式问题、OSS路径构建和缓存处理
 */

// OSS配置
const OSS_CONFIG = {
  domain: 'https://shiguang123418.oss-cn-hangzhou.aliyuncs.com',
  avatarPath: 'avatar/'
};

/**
 * 格式化头像URL
 * @param {string} url 原始URL
 * @returns {string} 格式化后的URL
 */
export function formatAvatarUrl(url) {
  if (!url) return '';
  
  console.log('处理头像URL:', url);
  
  // 已经是完整URL
  if (url.startsWith('http')) {
    console.log('URL是绝对路径，无需修改');
    return addTimestampToUrl(url);
  }
  
  // 相对路径，添加域名
  if (url.startsWith('/')) {
    console.log('URL是相对路径，添加域名');
    return addTimestampToUrl(window.location.origin + url);
  }
  
  // 其他情况，认为是OSS路径
  console.log('URL可能是OSS路径，添加OSS域名');
  
  // 确保包含avatar/前缀
  let ossPath = url;
  if (!ossPath.includes('avatar/')) {
    ossPath = OSS_CONFIG.avatarPath + ossPath;
  }
  
  // 添加OSS域名前缀
  return addTimestampToUrl(OSS_CONFIG.domain + '/' + ossPath.replace(/^\//, ''));
}

/**
 * 给URL添加时间戳，防止缓存
 * @param {string} url URL
 * @returns {string} 添加时间戳后的URL
 */
export function addTimestampToUrl(url) {
  if (!url) return '';
  
  const separator = url.includes('?') ? '&' : '?';
  return url + separator + 't=' + new Date().getTime();
}

/**
 * 从URL中提取文件名
 * @param {string} url URL
 * @returns {string} 文件名
 */
export function extractFilenameFromUrl(url) {
  if (!url) return '';
  
  // 移除查询参数
  const baseUrl = url.split('?')[0];
  
  // 提取最后一个 / 后的内容作为文件名
  const parts = baseUrl.split('/');
  return parts[parts.length - 1];
}

/**
 * 构建OSS头像URL
 * @param {string} filename 文件名
 * @param {string} datePath 可选的日期路径，格式如 '2023/05/26'
 * @returns {string} 完整的OSS URL
 */
export function buildOssAvatarUrl(filename, datePath = null) {
  if (!filename) return '';
  
  // 如果没有提供日期路径，使用当前日期
  if (!datePath) {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    datePath = `${year}/${month}/${day}`;
  }
  
  // 构建路径
  const path = `${OSS_CONFIG.avatarPath}${datePath}/${filename}`;
  
  // 返回完整URL
  return `${OSS_CONFIG.domain}/${path}`;
}

/**
 * 刷新头像URL (添加新的时间戳)
 * @param {string} url 原始URL
 * @returns {string} 刷新后的URL
 */
export function refreshAvatarUrl(url) {
  if (!url) return '';
  
  // 移除现有时间戳参数
  let baseUrl = url;
  if (url.includes('?')) {
    baseUrl = url.split('?')[0];
  }
  
  // 添加新的时间戳
  return addTimestampToUrl(baseUrl);
}

export default {
  formatAvatarUrl,
  addTimestampToUrl,
  extractFilenameFromUrl,
  buildOssAvatarUrl,
  refreshAvatarUrl
}; 