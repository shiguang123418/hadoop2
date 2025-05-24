package org.shiguang.module.common.controller;

import org.shiguang.module.common.response.ApiResponse;
import org.springframework.http.HttpStatus;

/**
 * 基础控制器
 * 提供所有控制器通用的方法
 */
public abstract class BaseController {

    /**
     * 创建成功响应
     * @param data 响应数据
     * @param <T> 数据类型
     * @return API响应对象
     */
    protected <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), "操作成功", data);
    }
    
    /**
     * 创建成功响应（无数据）
     * @return API响应对象
     */
    protected ApiResponse<Void> success() {
        return new ApiResponse<>(HttpStatus.OK.value(), "操作成功", null);
    }
    
    /**
     * 创建成功响应（自定义消息）
     * @param message 成功消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return API响应对象
     */
    protected <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), message, data);
    }
    
    /**
     * 创建错误响应
     * @param message 错误消息
     * @return API响应对象
     */
    protected ApiResponse<Void> error(String message) {
        return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, null);
    }
    
    /**
     * 创建错误响应（带返回类型）
     * @param message 错误消息
     * @param <T> 返回数据类型
     * @return API响应对象
     */
    protected <T> ApiResponse<T> error(String message, Class<T> clazz) {
        return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, null);
    }
    
    /**
     * 创建错误响应（自定义状态码）
     * @param code 状态码
     * @param message 错误消息
     * @return API响应对象
     */
    protected ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
    
    /**
     * 创建错误响应（自定义状态码，带返回类型）
     * @param code 状态码
     * @param message 错误消息
     * @param <T> 返回数据类型
     * @return API响应对象
     */
    protected <T> ApiResponse<T> error(int code, String message, Class<T> clazz) {
        return new ApiResponse<>(code, message, null);
    }
    
    /**
     * 创建未授权响应
     * @param message 错误消息
     * @return API响应对象
     */
    protected ApiResponse<Void> unauthorized(String message) {
        return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), message, null);
    }
    
    /**
     * 创建禁止访问响应
     * @param message 错误消息
     * @return API响应对象
     */
    protected ApiResponse<Void> forbidden(String message) {
        return new ApiResponse<>(HttpStatus.FORBIDDEN.value(), message, null);
    }
    
    /**
     * 创建资源不存在响应
     * @param message 错误消息
     * @return API响应对象
     */
    protected ApiResponse<Void> notFound(String message) {
        return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), message, null);
    }
} 