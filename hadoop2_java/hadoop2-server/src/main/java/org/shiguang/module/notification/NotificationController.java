package org.shiguang.module.notification;

import org.shiguang.module.common.response.PageResponse;
import org.shiguang.module.common.response.RestResponse;
import org.shiguang.entity.Notification;
import org.shiguang.entity.User;
import org.shiguang.module.audit.AuditOperation;
import org.shiguang.module.auth.service.AuthService;
import org.shiguang.entity.dto.NotificationSearchDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 通知消息控制器
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AuthService authService;
    
    /**
     * 获取当前用户的未读通知数量
     */
    @GetMapping("/unread-count")
    public RestResponse<Long> getUnreadCount() {
        long count = notificationService.getCurrentUserUnreadCount();
        return RestResponse.success(count);
    }
    
    /**
     * 获取当前用户的通知列表
     */
    @GetMapping("/my")
    public RestResponse<List<Notification>> getMyNotifications() {
        List<Notification> notifications = notificationService.getCurrentUserNotifications();
        return RestResponse.success(notifications);
    }
    
    /**
     * 获取当前用户的未读通知列表
     */
    @GetMapping("/my/unread")
    public RestResponse<List<Notification>> getMyUnreadNotifications() {
        List<Notification> notifications = notificationService.getCurrentUserUnreadNotifications();
        return RestResponse.success(notifications);
    }
    
    /**
     * 分页获取当前用户的通知列表
     */
    @GetMapping("/my/page")
    public RestResponse<PageResponse<Notification>> getMyNotificationsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
            
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Notification> notificationPage = notificationService.getCurrentUserNotificationsPaged(pageable);
        
        return RestResponse.success(new PageResponse<>(
            notificationPage.getContent(),
            notificationPage.getTotalElements(),
            notificationPage.getTotalPages(),
            notificationPage.getNumber(),
            notificationPage.getSize()
        ));
    }
    
    /**
     * 获取通知详情
     */
    @GetMapping("/{id}")
    public RestResponse<Notification> getNotification(@PathVariable Long id) {
        Optional<Notification> notification = notificationService.getNotification(id);
        
        if (notification.isPresent()) {
            Notification notif = notification.get();
            
            // 验证权限：只能查看自己的通知或系统通知
            User currentUser = authService.getCurrentUser();
            if (currentUser != null && (notif.getUserId() == null || notif.getUserId().equals(currentUser.getId()))) {
                return RestResponse.success(notif);
            }
            // 管理员可以查看所有通知
            else if (authService.hasRole("ROLE_ADMIN")) {
                return RestResponse.success(notif);
            }
            
            return RestResponse.fail("无权访问该通知");
        }
        
        return RestResponse.fail("通知不存在");
    }
    
    /**
     * 标记通知为已读
     */
    @PutMapping("/{id}/read")
    @AuditOperation(operation = "标记通知为已读", operationType = "UPDATE", resourceType = "NOTIFICATION", resourceIdIndex = 0)
    public RestResponse<Boolean> markAsRead(@PathVariable Long id) {
        Optional<Notification> notificationOpt = notificationService.getNotification(id);
        
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            
            // 验证权限：只能操作自己的通知或系统通知
            User currentUser = authService.getCurrentUser();
            if (currentUser != null && (notification.getUserId() == null || notification.getUserId().equals(currentUser.getId()))) {
                boolean success = notificationService.markAsRead(id);
                return success ? RestResponse.success(true) : RestResponse.fail("操作失败");
            }
            
            return RestResponse.fail("无权操作该通知");
        }
        
        return RestResponse.fail("通知不存在");
    }
    
    /**
     * 标记通知为已确认
     */
    @PutMapping("/{id}/confirm")
    @AuditOperation(operation = "确认通知", operationType = "UPDATE", resourceType = "NOTIFICATION", resourceIdIndex = 0)
    public RestResponse<Boolean> markAsConfirmed(@PathVariable Long id) {
        Optional<Notification> notificationOpt = notificationService.getNotification(id);
        
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            
            // 验证权限：只能操作自己的通知或系统通知
            User currentUser = authService.getCurrentUser();
            if (currentUser != null && (notification.getUserId() == null || notification.getUserId().equals(currentUser.getId()))) {
                if (!notification.isNeedConfirm()) {
                    return RestResponse.fail("此通知无需确认");
                }
                
                boolean success = notificationService.markAsConfirmed(id);
                return success ? RestResponse.success(true) : RestResponse.fail("操作失败");
            }
            
            return RestResponse.fail("无权操作该通知");
        }
        
        return RestResponse.fail("通知不存在");
    }
    
    /**
     * 标记所有通知为已读
     */
    @PutMapping("/mark-all-read")
    @AuditOperation(operation = "标记所有通知为已读", operationType = "UPDATE", resourceType = "NOTIFICATION")
    public RestResponse<Integer> markAllAsRead() {
        int count = notificationService.markAllAsRead();
        return RestResponse.success(count);
    }
    
    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    @AuditOperation(operation = "删除通知", operationType = "DELETE", resourceType = "NOTIFICATION", resourceIdIndex = 0)
    public RestResponse<Boolean> deleteNotification(@PathVariable Long id) {
        Optional<Notification> notificationOpt = notificationService.getNotification(id);
        
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            
            // 验证权限：只能操作自己的通知
            User currentUser = authService.getCurrentUser();
            if (currentUser != null && notification.getUserId() != null && notification.getUserId().equals(currentUser.getId())) {
                boolean success = notificationService.deleteNotification(id);
                return success ? RestResponse.success(true) : RestResponse.fail("删除失败");
            } 
            // 管理员可以删除所有通知
            else if (authService.hasRole("ROLE_ADMIN")) {
                boolean success = notificationService.deleteNotification(id);
                return success ? RestResponse.success(true) : RestResponse.fail("删除失败");
            }
            
            return RestResponse.fail("无权删除该通知");
        }
        
        return RestResponse.fail("通知不存在");
    }
    
    /**
     * 批量删除通知
     */
    @DeleteMapping("/batch")
    @AuditOperation(operation = "批量删除通知", operationType = "DELETE", resourceType = "NOTIFICATION")
    public RestResponse<Integer> batchDeleteNotifications(@RequestBody List<Long> ids) {
        // 管理员才能批量删除
        if (authService.hasRole("ROLE_ADMIN")) {
            int count = notificationService.deleteNotifications(ids);
            return RestResponse.success(count);
        }
        
        return RestResponse.fail("无权执行批量删除");
    }
    
    /**
     * 清空所有通知
     */
    @DeleteMapping("/clear-all")
    @AuditOperation(operation = "清空所有通知", operationType = "DELETE", resourceType = "NOTIFICATION")
    public RestResponse<Integer> clearAllNotifications() {
        int count = notificationService.clearCurrentUserNotifications();
        return RestResponse.success(count);
    }
    
    /**
     * 管理员：创建系统通知
     */
    @PostMapping("/system")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @AuditOperation(operation = "创建系统通知", operationType = "CREATE", resourceType = "NOTIFICATION")
    public RestResponse<Notification> createSystemNotification(@RequestBody Notification notification) {
        Notification created = notificationService.createSystemNotification(
            notification.getTitle(),
            notification.getContent(),
            notification.getType(),
            notification.getLevel(),
            notification.getSource()
        );
        
        return RestResponse.success(created);
    }
    
    /**
     * 管理员：创建用户通知
     */
    @PostMapping("/user")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @AuditOperation(operation = "创建用户通知", operationType = "CREATE", resourceType = "NOTIFICATION")
    public RestResponse<Notification> createUserNotification(@RequestBody Notification notification) {
        if (notification.getUserId() == null) {
            return RestResponse.fail("用户ID不能为空");
        }
        
        Notification created = notificationService.createUserNotification(
            notification.getTitle(),
            notification.getContent(),
            notification.getType(),
            notification.getLevel(),
            notification.getSource(),
            notification.getUserId(),
            notification.getUsername()
        );
        
        return RestResponse.success(created);
    }
    
    /**
     * 管理员：高级搜索通知
     */
    @PostMapping("/search")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public RestResponse<PageResponse<Notification>> searchNotifications(
        @RequestBody NotificationSearchDTO searchDTO,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<Notification> notificationPage = notificationService.searchNotifications(
            searchDTO.getUserId(),
            searchDTO.getType(),
            searchDTO.getLevel(),
            searchDTO.getSource(),
            searchDTO.getRead(),
            searchDTO.getNeedConfirm(),
            searchDTO.getConfirmed(),
            searchDTO.getStartDate(),
            searchDTO.getEndDate(),
            pageable
        );
        
        return RestResponse.success(new PageResponse<>(
            notificationPage.getContent(),
            notificationPage.getTotalElements(),
            notificationPage.getTotalPages(),
            notificationPage.getNumber(),
            notificationPage.getSize()
        ));
    }
    
    /**
     * 获取通知统计信息
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public RestResponse<Map<String, Object>> getNotificationStats() {
        Map<String, Object> stats = new HashMap<>();
        
        User currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            Long userId = currentUser.getId();
            
            stats.put("totalCount", notificationService.getUserNotifications(userId).size());
            stats.put("unreadCount", notificationService.getUserUnreadCount(userId));
            
            // TODO: 扩展更多的统计信息，如按类型、级别统计等
            // 由于这些需要额外的存储库方法和服务方法，此处暂未实现
        }
        
        return RestResponse.success(stats);
    }
} 