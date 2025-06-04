package org.shiguang.module.common.response;

/**
 * REST API通用响应封装类
 */
public class RestResponse<T> {

    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 响应码
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

    public RestResponse() {
    }

    public RestResponse(boolean success, int code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应
     */
    public static <T> RestResponse<T> success(T data) {
        return new RestResponse<>(true, 200, "操作成功", data);
    }

    /**
     * 创建成功响应（带消息）
     */
    public static <T> RestResponse<T> success(String message, T data) {
        return new RestResponse<>(true, 200, message, data);
    }

    /**
     * 创建失败响应
     */
    public static <T> RestResponse<T> fail(String message) {
        return new RestResponse<>(false, 400, message, null);
    }

    /**
     * 创建失败响应（带响应码）
     */
    public static <T> RestResponse<T> fail(int code, String message) {
        return new RestResponse<>(false, code, message, null);
    }

    /**
     * 创建失败响应（带数据）
     */
    public static <T> RestResponse<T> fail(String message, T data) {
        return new RestResponse<>(false, 400, message, data);
    }

    /**
     * 创建失败响应（带响应码和数据）
     */
    public static <T> RestResponse<T> fail(int code, String message, T data) {
        return new RestResponse<>(false, code, message, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

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
} 