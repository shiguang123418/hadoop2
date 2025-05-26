package org.shiguang.module.notification;

import org.shiguang.entity.Notification;
import org.shiguang.entity.User;
import org.shiguang.module.audit.AuditOperation;
import org.shiguang.module.auth.service.AuthService;
import org.shiguang.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 通知服务
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private AuthService authService;
    
    // 可选：WebSocket支持，如果启用了WebSocket功能，可以推送通知
    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 创建用户通知
     */
    @Transactional
    public Notification createUserNotification(String title, String content, String type, String level, 
                                            String source, Long userId, String username) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type); // INFO, WARNING, ERROR, SUCCESS
        notification.setLevel(level); // LOW, MEDIUM, HIGH, URGENT
        notification.setSource(source);
        notification.setUserId(userId);
        notification.setUsername(username);
        notification.setCreatedAt(LocalDateTime.now());
        
        Notification saved = notificationRepository.save(notification);
        
        // 如果启用了WebSocket，则推送通知到客户端
        if (messagingTemplate != null) {
            try {
                // 异步推送，避免阻塞
                CompletableFuture.runAsync(() -> {
                    try {
                        // 推送到用户专属频道
                        messagingTemplate.convertAndSendToUser(
                            userId.toString(), 
                            "/queue/notifications", 
                            saved
                        );
                    } catch (Exception e) {
                        logger.error("推送通知失败: " + e.getMessage(), e);
                    }
                });
            } catch (Exception e) {
                logger.error("创建异步任务失败: " + e.getMessage(), e);
            }
        }
        
        return saved;
    }
    
    /**
     * 创建系统通知（发送给所有用户）
     */
    @Transactional
    public Notification createSystemNotification(String title, String content, String type, String level, 
                                              String source) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setLevel(level);
        notification.setSource(source);
        notification.setCreatedAt(LocalDateTime.now());
        
        Notification saved = notificationRepository.save(notification);
        
        // 如果启用了WebSocket，则推送通知到客户端
        if (messagingTemplate != null) {
            try {
                // 异步推送，避免阻塞
                CompletableFuture.runAsync(() -> {
                    try {
                        // 推送到系统广播频道
                        messagingTemplate.convertAndSend("/topic/system-notifications", saved);
                    } catch (Exception e) {
                        logger.error("推送系统通知失败: " + e.getMessage(), e);
                    }
                });
            } catch (Exception e) {
                logger.error("创建异步任务失败: " + e.getMessage(), e);
            }
        }
        
        return saved;
    }
    
    /**
     * 创建需要确认的通知
     */
    @Transactional
    public Notification createConfirmationNotification(String title, String content, String type, String level, 
                                                    String source, Long userId, String username) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setLevel(level);
        notification.setSource(source);
        notification.setUserId(userId);
        notification.setUsername(username);
        notification.setNeedConfirm(true);
        notification.setCreatedAt(LocalDateTime.now());
        
        Notification saved = notificationRepository.save(notification);
        
        // 如果启用了WebSocket，则推送通知到客户端
        if (messagingTemplate != null) {
            try {
                CompletableFuture.runAsync(() -> {
                    try {
                        messagingTemplate.convertAndSendToUser(
                            userId.toString(), 
                            "/queue/notifications", 
                            saved
                        );
                    } catch (Exception e) {
                        logger.error("推送确认通知失败: " + e.getMessage(), e);
                    }
                });
            } catch (Exception e) {
                logger.error("创建异步任务失败: " + e.getMessage(), e);
            }
        }
        
        return saved;
    }
    
    /**
     * 创建与资源关联的通知
     */
    @Transactional
    public Notification createResourceNotification(String title, String content, String type, String level,
                                               String source, Long userId, String username,
                                               String resourceType, String resourceId, String link) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setLevel(level);
        notification.setSource(source);
        notification.setUserId(userId);
        notification.setUsername(username);
        notification.setResourceType(resourceType);
        notification.setResourceId(resourceId);
        notification.setLink(link);
        notification.setCreatedAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }
    
    /**
     * 获取当前用户的通知
     */
    public List<Notification> getCurrentUserNotifications() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Collections.emptyList();
        }
        
        return notificationRepository.findByUserIdOrSystemNotifications(currentUser.getId());
    }
    
    /**
     * 获取当前用户的未读通知
     */
    public List<Notification> getCurrentUserUnreadNotifications() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Collections.emptyList();
        }
        
        return notificationRepository.findUnreadByUserIdOrSystemNotifications(currentUser.getId());
    }
    
    /**
     * 获取指定用户的通知列表
     */
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrSystemNotifications(userId);
    }
    
    /**
     * 获取指定用户的未读通知列表
     */
    public List<Notification> getUserUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadByUserIdOrSystemNotifications(userId);
    }
    
    /**
     * 分页获取当前用户的通知
     */
    public Page<Notification> getCurrentUserNotificationsPaged(Pageable pageable) {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Page.empty(pageable);
        }
        
        return notificationRepository.findByUserIdOrSystemNotificationsPageable(currentUser.getId(), pageable);
    }
    
    /**
     * 分页获取指定用户的通知
     */
    public Page<Notification> getUserNotificationsPaged(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrSystemNotificationsPageable(userId, pageable);
    }
    
    /**
     * 高级搜索通知
     */
    public Page<Notification> searchNotifications(Long userId, String type, String level, String source,
                                             Boolean read, Boolean needConfirm, Boolean confirmed,
                                             LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return notificationRepository.searchNotifications(
            userId, type, level, source, read, needConfirm, confirmed, startDate, endDate, pageable
        );
    }
    
    /**
     * 获取通知详情
     */
    public Optional<Notification> getNotification(Long id) {
        return notificationRepository.findById(id);
    }
    
    /**
     * 获取当前用户的未读通知数量
     */
    public long getCurrentUserUnreadCount() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return 0;
        }
        
        return notificationRepository.countUnreadByUserIdOrSystemNotifications(currentUser.getId());
    }
    
    /**
     * 获取指定用户的未读通知数量
     */
    public long getUserUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserIdOrSystemNotifications(userId);
    }
    
    /**
     * 将通知标记为已读
     */
    @Transactional
    @AuditOperation(operation = "标记通知为已读", operationType = "UPDATE", resourceType = "NOTIFICATION", resourceIdIndex = 0)
    public boolean markAsRead(Long id) {
        int updatedCount = notificationRepository.markAsRead(id, LocalDateTime.now());
        return updatedCount > 0;
    }
    
    /**
     * 将通知标记为已确认
     */
    @Transactional
    @AuditOperation(operation = "确认通知", operationType = "UPDATE", resourceType = "NOTIFICATION", resourceIdIndex = 0)
    public boolean markAsConfirmed(Long id) {
        int updatedCount = notificationRepository.markAsConfirmed(id, LocalDateTime.now());
        return updatedCount > 0;
    }
    
    /**
     * 将当前用户的所有通知标记为已读
     */
    @Transactional
    @AuditOperation(operation = "标记所有通知为已读", operationType = "UPDATE", resourceType = "NOTIFICATION")
    public int markAllAsRead() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return 0;
        }
        
        return notificationRepository.markAllAsRead(currentUser.getId(), LocalDateTime.now());
    }
    
    /**
     * 逻辑删除通知
     */
    @Transactional
    @AuditOperation(operation = "删除通知", operationType = "DELETE", resourceType = "NOTIFICATION", resourceIdIndex = 0)
    public boolean deleteNotification(Long id) {
        int updatedCount = notificationRepository.logicalDelete(id);
        return updatedCount > 0;
    }
    
    /**
     * 批量逻辑删除通知
     */
    @Transactional
    @AuditOperation(operation = "批量删除通知", operationType = "DELETE", resourceType = "NOTIFICATION")
    public int deleteNotifications(List<Long> ids) {
        return notificationRepository.batchLogicalDelete(ids);
    }
    
    /**
     * 清空当前用户的所有通知
     */
    @Transactional
    @AuditOperation(operation = "清空所有通知", operationType = "DELETE", resourceType = "NOTIFICATION")
    public int clearCurrentUserNotifications() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return 0;
        }
        
        return notificationRepository.clearUserNotifications(currentUser.getId());
    }
} 