package org.shiguang.module.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 审计日志AOP切面
 */
@Aspect
@Component
public class AuditLogAspect {

    @Autowired
    private AuditLogService auditLogService;

    /**
     * 环绕通知，拦截带有@AuditOperation注解的方法
     */
    @Around("@annotation(org.shiguang.module.audit.AuditOperation)")
    public Object auditLog(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取当前用户
        String username = getCurrentUsername();
        
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 获取注解信息
        AuditOperation auditOperation = method.getAnnotation(AuditOperation.class);
        String operation = auditOperation.operation();
        String operationType = auditOperation.operationType();
        String resourceType = auditOperation.resourceType();
        
        // 提取资源ID（如果有）
        String resourceId = extractResourceId(joinPoint, auditOperation);
        
        // 记录方法参数作为详情
        String details = extractMethodDetails(joinPoint);
        
        // 执行原方法
        Object result;
        try {
            result = joinPoint.proceed();
            
            // 记录审计日志
            auditLogService.createAuditLog(
                username,
                operation,
                operationType,
                resourceType,
                resourceId,
                details
            );
            
            return result;
        } catch (Throwable throwable) {
            // 记录异常审计日志
            auditLogService.createAuditLog(
                username,
                operation + " 失败",
                operationType,
                resourceType,
                resourceId,
                details + ", 异常: " + throwable.getMessage()
            );
            
            throw throwable;
        }
    }
    
    /**
     * 获取当前登录用户
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "系统";
    }
    
    /**
     * 提取资源ID
     */
    private String extractResourceId(ProceedingJoinPoint joinPoint, AuditOperation auditOperation) {
        // 从方法参数中提取资源ID
        int resourceIdIndex = auditOperation.resourceIdIndex();
        if (resourceIdIndex >= 0 && joinPoint.getArgs().length > resourceIdIndex) {
            Object resourceIdObj = joinPoint.getArgs()[resourceIdIndex];
            if (resourceIdObj != null) {
                return resourceIdObj.toString();
            }
        }
        return "";
    }
    
    /**
     * 提取方法详情
     */
    private String extractMethodDetails(ProceedingJoinPoint joinPoint) {
        StringBuilder details = new StringBuilder();
        Object[] args = joinPoint.getArgs();
        
        details.append("参数: [");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                details.append(", ");
            }
            
            Object arg = args[i];
            if (arg == null) {
                details.append("null");
            } else if (arg.getClass().isPrimitive() || arg instanceof String || arg instanceof Number) {
                details.append(arg);
            } else {
                details.append(arg.getClass().getSimpleName());
            }
        }
        details.append("]");
        
        return details.toString();
    }
} 