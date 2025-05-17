package org.shiguang.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Hadoop禁用条件
 * 当hadoop.enabled=false或未配置时，此条件为true
 */
public class HadoopDisabledCondition implements Condition {
    
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String enabled = context.getEnvironment().getProperty("hadoop.enabled");
        // 默认禁用
        if (enabled == null) {
            return true;
        }
        return !Boolean.parseBoolean(enabled);
    }
} 