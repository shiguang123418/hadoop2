package org.shiguang.module.audit;

import org.shiguang.entity.AuditLog;
import org.shiguang.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 审计日志服务
 */
@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * 创建审计日志
     */
    @Transactional
    public AuditLog createAuditLog(String username, String operation, String operationType,
                                 String resourceType, String resourceId, String details) {
        String clientIp = getClientIpAddress();
        
        AuditLog auditLog = new AuditLog(
            username,
            operation,
            operationType,
            resourceType,
            resourceId,
            details,
            clientIp
        );
        
        return auditLogRepository.save(auditLog);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return "未知IP";
    }

    /**
     * 查询审计日志
     */
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    /**
     * 按ID查询审计日志
     */
    public Optional<AuditLog> getAuditLogById(Long id) {
        return auditLogRepository.findById(id);
    }

    /**
     * 按用户名查询审计日志
     */
    public Page<AuditLog> getAuditLogsByUsername(String username, Pageable pageable) {
        return auditLogRepository.findByUsername(username, pageable);
    }

    /**
     * 按操作类型查询审计日志
     */
    public Page<AuditLog> getAuditLogsByOperationType(String operationType, Pageable pageable) {
        return auditLogRepository.findByOperationType(operationType, pageable);
    }

    /**
     * 按资源类型查询审计日志
     */
    public Page<AuditLog> getAuditLogsByResourceType(String resourceType, Pageable pageable) {
        return auditLogRepository.findByResourceType(resourceType, pageable);
    }

    /**
     * 按时间范围查询审计日志
     */
    public Page<AuditLog> getAuditLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return auditLogRepository.findByCreatedAtBetween(startTime, endTime, pageable);
    }

    /**
     * 高级搜索审计日志
     */
    public Page<AuditLog> searchAuditLogs(String username, String operationType, String resourceType,
                                       LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return auditLogRepository.searchAuditLogs(username, operationType, resourceType, startTime, endTime, pageable);
    }

    /**
     * 按ID删除审计日志
     */
    @Transactional
    public void deleteById(Long id) {
        auditLogRepository.deleteById(id);
    }

    /**
     * 统计审计日志数量
     */
    public long count() {
        return auditLogRepository.count();
    }
} 