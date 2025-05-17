package org.shiguang.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 控制Spark相关Bean是否创建的条件
 */
public class SparkEnabledCondition implements Condition {
    
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String enabled = context.getEnvironment().getProperty("spark.enabled");
        // 默认不启用
        if (enabled == null) {
            return false;
        }
        return "true".equalsIgnoreCase(enabled);
    }
} 