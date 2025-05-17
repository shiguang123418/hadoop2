package org.shiguang.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Kafka禁用条件
 * 当kafka.enabled=false或未配置时，此条件为true
 */
public class KafkaDisabledCondition implements Condition {
    
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String enabled = context.getEnvironment().getProperty("kafka.enabled");
        // 默认禁用
        if (enabled == null) {
            return true;
        }
        return !Boolean.parseBoolean(enabled);
    }
} 