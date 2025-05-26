package org.shiguang.module.notification.constants;

/**
 * 通知相关常量
 */
public class NotificationConstants {

    /**
     * 通知类型
     */
    public static class Type {
        /**
         * 信息类通知
         */
        public static final String INFO = "INFO";
        
        /**
         * 警告类通知
         */
        public static final String WARNING = "WARNING";
        
        /**
         * 错误类通知
         */
        public static final String ERROR = "ERROR";
        
        /**
         * 成功类通知
         */
        public static final String SUCCESS = "SUCCESS";
    }
    
    /**
     * 通知级别
     */
    public static class Level {
        /**
         * 低级别
         */
        public static final String LOW = "LOW";
        
        /**
         * 中级别
         */
        public static final String MEDIUM = "MEDIUM";
        
        /**
         * 高级别
         */
        public static final String HIGH = "HIGH";
        
        /**
         * 紧急级别
         */
        public static final String URGENT = "URGENT";
    }
    
    /**
     * 通知来源/模块
     */
    public static class Source {
        /**
         * 系统
         */
        public static final String SYSTEM = "SYSTEM";
        
        /**
         * 安全
         */
        public static final String SECURITY = "SECURITY";
        
        /**
         * 用户管理
         */
        public static final String USER = "USER";
        
        /**
         * 任务管理
         */
        public static final String TASK = "TASK";
        
        /**
         * 数据管理
         */
        public static final String DATA = "DATA";
        
        /**
         * 审计日志
         */
        public static final String AUDIT = "AUDIT";
    }
    
    /**
     * 资源类型
     */
    public static class ResourceType {
        /**
         * 用户
         */
        public static final String USER = "USER";
        
        /**
         * 任务
         */
        public static final String TASK = "TASK";
        
        /**
         * 数据集
         */
        public static final String DATASET = "DATASET";
        
        /**
         * 系统配置
         */
        public static final String CONFIG = "CONFIG";
        
        /**
         * 模型
         */
        public static final String MODEL = "MODEL";
    }
} 