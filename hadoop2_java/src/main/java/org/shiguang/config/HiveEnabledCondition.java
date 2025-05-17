package org.shiguang.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Hive启用条件
 * 当hive.enabled=true时，此条件为true
 */
public class HiveEnabledCondition implements Condition {
    
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String enabled = context.getEnvironment().getProperty("hive.enabled");
        // 默认不启用
        if (enabled == null) {
            return false;
        }
        return "true".equalsIgnoreCase(enabled);
    }
} 