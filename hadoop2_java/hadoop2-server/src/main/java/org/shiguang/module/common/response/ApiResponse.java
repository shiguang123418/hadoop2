package org.shiguang.module.common.response;

import java.io.Serializable;

/**
 * 统一API响应结果类
 * @param <T> 响应数据类型
 */
public class ApiResponse<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 状态码
     */
    private int code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private long timestamp;
    
    /**
     * 构造函数
     * @param code 状态码
     * @param message 响应消息
     * @param data 响应数据
     */
    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 构造函数
     * @param code 状态码
     * @param message 响应消息
     */
    public ApiResponse(int code, String message) {
        this(code, message, null);
    }
    
    /**
     * 构造函数
     */
    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 创建成功响应
     * @param data 响应数据
     * @param <T> 数据类型
     * @return API响应对象
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }
    
    /**
     * 创建成功响应（自定义消息）
     * @param message 响应消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return API响应对象
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }
    
    /**
     * 创建成功响应（无数据）
     * @return API响应对象
     */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(200, "操作成功");
    }
    
    /**
     * 创建失败响应（自定义消息）
     * @param code 状态码
     * @param message 响应消息
     * @return API响应对象
     */
    public static ApiResponse<Void> fail(int code, String message) {
        return new ApiResponse<>(code, message);
    }
    
    /**
     * 创建失败响应
     * @param message 响应消息
     * @return API响应对象
     */
    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(500, message);
    }
    
    // Getter和Setter

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