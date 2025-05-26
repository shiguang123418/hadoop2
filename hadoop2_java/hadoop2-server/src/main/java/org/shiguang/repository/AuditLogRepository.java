package org.shiguang.repository;

import org.shiguang.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志仓库
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    /**
     * 按用户名查询审计日志
     */
    Page<AuditLog> findByUsername(String username, Pageable pageable);
    
    /**
     * 按操作类型查询审计日志
     */
    Page<AuditLog> findByOperationType(String operationType, Pageable pageable);
    
    /**
     * 按资源类型查询审计日志
     */
    Page<AuditLog> findByResourceType(String resourceType, Pageable pageable);
    
    /**
     * 按时间范围查询审计日志
     */
    Page<AuditLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 高级搜索
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:username IS NULL OR a.username LIKE %:username%) AND " +
           "(:operationType IS NULL OR a.operationType = :operationType) AND " +
           "(:resourceType IS NULL OR a.resourceType = :resourceType) AND " +
           "(:startTime IS NULL OR a.createdAt >= :startTime) AND " +
           "(:endTime IS NULL OR a.createdAt <= :endTime)")
    Page<AuditLog> searchAuditLogs(
        @Param("username") String username,
        @Param("operationType") String operationType,
        @Param("resourceType") String resourceType,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable);
} 