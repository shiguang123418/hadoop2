package org.shiguang.module.datasource.exception;

/**
 * 数据源不存在异常
 */
public class DataSourceNotFoundException extends DataSourceException {
    public DataSourceNotFoundException(String message) {
        super(message);
    }
    
    public DataSourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 