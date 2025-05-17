package org.shiguang.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务异常类
 * 用于表示业务逻辑错误
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    /**
     * 创建一个业务异常
     *
     * @param message 错误消息
     * @param status HTTP状态码
     * @param errorCode 错误代码
     */
    public BusinessException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    /**
     * 创建一个业务异常，默认HTTP状态码为BAD_REQUEST
     *
     * @param message 错误消息
     * @param errorCode 错误代码
     */
    public BusinessException(String message, String errorCode) {
        this(message, HttpStatus.BAD_REQUEST, errorCode);
    }

    /**
     * 创建一个业务异常，默认HTTP状态码为BAD_REQUEST，错误代码为BUSINESS_ERROR
     *
     * @param message 错误消息
     */
    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST, "BUSINESS_ERROR");
    }
} 