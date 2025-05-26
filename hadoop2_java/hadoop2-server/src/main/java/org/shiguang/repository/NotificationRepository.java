package org.shiguang.repository;

import org.shiguang.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知消息数据访问接口
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 获取用户的通知列表(包括系统通知)
     * 逻辑未删除 AND (用户ID为指定ID OR 用户ID为空-系统通知)
     */
    @Query("SELECT n FROM Notification n WHERE n.deleted = false AND (n.userId = :userId OR n.userId IS NULL) ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrSystemNotifications(@Param("userId") Long userId);

    /**
     * 获取用户的未读通知列表(包括系统通知)
     */
    @Query("SELECT n FROM Notification n WHERE n.deleted = false AND n.read = false AND (n.userId = :userId OR n.userId IS NULL) ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserIdOrSystemNotifications(@Param("userId") Long userId);
    
    /**
     * 查询指定用户的通知总数
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.deleted = false AND (n.userId = :userId OR n.userId IS NULL)")
    long countByUserIdOrSystemNotifications(@Param("userId") Long userId);
    
    /**
     * 查询指定用户的未读通知总数
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.deleted = false AND n.read = false AND (n.userId = :userId OR n.userId IS NULL)")
    long countUnreadByUserIdOrSystemNotifications(@Param("userId") Long userId);
    
    /**
     * 分页查询用户通知
     */
    @Query("SELECT n FROM Notification n WHERE n.deleted = false AND (n.userId = :userId OR n.userId IS NULL) ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdOrSystemNotificationsPageable(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 高级搜索通知
     */
    @Query("SELECT n FROM Notification n WHERE " +
           "n.deleted = false AND " +
           "((:userId IS NULL) OR (n.userId = :userId OR n.userId IS NULL)) AND " +
           "(:type IS NULL OR n.type = :type) AND " +
           "(:level IS NULL OR n.level = :level) AND " +
           "(:source IS NULL OR n.source = :source) AND " +
           "(:read IS NULL OR n.read = :read) AND " +
           "(:needConfirm IS NULL OR n.needConfirm = :needConfirm) AND " +
           "(:confirmed IS NULL OR n.confirmed = :confirmed) AND " +
           "(:startDate IS NULL OR n.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR n.createdAt <= :endDate)")
    Page<Notification> searchNotifications(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("level") String level,
            @Param("source") String source,
            @Param("read") Boolean read,
            @Param("needConfirm") Boolean needConfirm,
            @Param("confirmed") Boolean confirmed,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
    
    /**
     * 将通知标记为已读
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.id = :id AND n.read = false")
    int markAsRead(@Param("id") Long id, @Param("readAt") LocalDateTime readAt);
    
    /**
     * 将通知标记为已确认
     */
    @Modifying
    @Query("UPDATE Notification n SET n.confirmed = true, n.confirmedAt = :confirmedAt WHERE n.id = :id AND n.needConfirm = true AND n.confirmed = false")
    int markAsConfirmed(@Param("id") Long id, @Param("confirmedAt") LocalDateTime confirmedAt);
    
    /**
     * 将用户的所有通知标记为已读
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE (n.userId = :userId OR n.userId IS NULL) AND n.read = false AND n.deleted = false")
    int markAllAsRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
    
    /**
     * 逻辑删除通知
     */
    @Modifying
    @Query("UPDATE Notification n SET n.deleted = true WHERE n.id = :id")
    int logicalDelete(@Param("id") Long id);
    
    /**
     * 批量逻辑删除通知
     */
    @Modifying
    @Query("UPDATE Notification n SET n.deleted = true WHERE n.id IN :ids")
    int batchLogicalDelete(@Param("ids") List<Long> ids);
    
    /**
     * 清除用户的所有通知(逻辑删除)
     */
    @Modifying
    @Query("UPDATE Notification n SET n.deleted = true WHERE n.userId = :userId AND n.deleted = false")
    int clearUserNotifications(@Param("userId") Long userId);
} 