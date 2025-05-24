package org.shiguang.entity.dto;

import org.shiguang.utils.AppConstants;

/**
 * 统一API响应格式
 * @param <T> 响应数据类型
 */
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;

    /**
     * 构造函数
     * @param code 状态码
     * @param message 消息
     * @param data 数据
     */
    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建成功响应
     * @param message 成功消息
     * @param data 数据
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(AppConstants.API_SUCCESS_CODE, message, data);
    }

    /**
     * 创建成功响应（无数据）
     * @param message 成功消息
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(AppConstants.API_SUCCESS_CODE, message, null);
    }

    /**
     * 创建错误响应
     * @param code 错误码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 错误响应
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 创建错误响应（使用默认错误码）
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 错误响应
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(AppConstants.API_ERROR_CODE, message, null);
    }

    // Getters and Setters
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
} 