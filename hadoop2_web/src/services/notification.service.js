import ApiService from './api.service';

/**
 * 通知服务 - 提供通知相关的API调用方法
 */
class NotificationService extends ApiService {
  constructor() {
    // 使用'notifications'作为基础路径
    super('notifications');
  }
  
  /**
   * 获取当前用户的通知列表（分页）
   */
  getMyNotificationsPage(page = 0, size = 10, sortBy = 'createdAt', sortDirection = 'DESC', filters = {}) {
    const params = {
      page,
      size,
      sortBy,
      sortDirection,
      ...filters
    };
    
    return this.get('/my/page', params);
  }
  
  /**
   * 获取当前用户的未读通知列表
   */
  getMyUnreadNotifications(filters = {}) {
    return this.get('/my/unread', filters);
  }
  
  /**
   * 获取当前用户的未读通知数量
   */
  getUnreadCount() {
    return this.get('/unread-count');
  }
  
  /**
   * 标记通知为已读
   */
  markAsRead(notificationId) {
    if (!notificationId) {
      return Promise.reject(new Error('通知ID不能为空'));
    }
    
    return this.put(`/${notificationId}/read`);
  }
  
  /**
   * 标记通知为已确认
   */
  markAsConfirmed(notificationId) {
    if (!notificationId) {
      return Promise.reject(new Error('通知ID不能为空'));
    }
    
    return this.put(`/${notificationId}/confirm`);
  }
  
  /**
   * 删除通知
   */
  deleteNotification(notificationId) {
    if (!notificationId) {
      return Promise.reject(new Error('通知ID不能为空'));
    }
    
    return this.delete(`/${notificationId}`);
  }
  
  /**
   * 标记所有通知为已读
   */
  markAllAsRead() {
    return this.put('/mark-all-read');
  }
  
  /**
   * 清空当前用户的所有通知
   */
  clearAllNotifications() {
    return this.delete('/clear-all');
  }
  
  /**
   * 批量删除通知
   */
  batchDeleteNotifications(ids) {
    if (!ids || !Array.isArray(ids) || ids.length === 0) {
      return Promise.reject(new Error('通知ID列表不能为空'));
    }
    
    return this.delete('/batch', {}, { data: ids });
  }
  
  /**
   * 搜索通知（管理员功能）
   */
  searchNotifications(searchForm, page = 0, size = 10, sortBy = 'createdAt', sortDirection = 'DESC') {
    const params = {
      page,
      size,
      sortBy,
      sortDirection
    };
    
    return this.post('/search', searchForm, { params });
  }
  
  /**
   * 创建系统通知（管理员功能）
   */
  createSystemNotification(notification) {
    return this.post('/system', notification);
  }
  
  /**
   * 创建用户通知（管理员功能）
   */
  createUserNotification(notification) {
    return this.post('/user', notification);
  }
}

// 导出通知服务实例
export default new NotificationService(); 