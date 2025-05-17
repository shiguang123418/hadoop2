package org.shiguang.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Spark禁用条件
 * 当spark.enabled=false或未配置时，此条件为true
 */
public class SparkDisabledCondition implements Condition {
    
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String enabled = context.getEnvironment().getProperty("spark.enabled");
        // 默认禁用
        if (enabled == null) {
            return true;
        }
        return !Boolean.parseBoolean(enabled);
    }
} 