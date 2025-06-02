package com.hadoop2.exception;

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