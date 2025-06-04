package org.shiguang.module.datasource.exception;

/**
 * 数据源通用异常
 */
public class DataSourceException extends RuntimeException {
    public DataSourceException(String message) {
        super(message);
    }
    
    public DataSourceException(String message, Throwable cause) {
        super(message, cause);
    }
} 