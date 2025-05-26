package org.shiguang.module.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计操作注解，用于标记需要记录审计日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditOperation {
    /**
     * 操作名称，如"创建用户"、"修改配置"等
     */
    String operation();
    
    /**
     * 操作类型，如"CREATE"、"UPDATE"、"DELETE"、"QUERY"等
     */
    String operationType();
    
    /**
     * 资源类型，如"USER"、"CONFIGURATION"、"FILE"等
     */
    String resourceType();
    
    /**
     * 资源ID在方法参数中的索引位置
     * 如果为-1表示无需提取资源ID
     */
    int resourceIdIndex() default -1;
} 