package org.shiguang.module.common.exception;

import org.shiguang.entity.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.validation.ConstraintViolationException;
import java.io.IOException;

/**
 * 全局异常处理器
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ApiResponse<String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        logger.warn("参数验证失败: {}", errorMessage);
        return ApiResponse.error(400, "参数错误: " + errorMessage);
    }
    
    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ApiResponse<String> handleConstraintViolation(ConstraintViolationException ex) {
        String errorMessage = ex.getConstraintViolations().iterator().next().getMessage();
        logger.warn("约束违反: {}", errorMessage);
        return ApiResponse.error(400, "参数错误: " + errorMessage);
    }
    
    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public ApiResponse<String> handleAuthenticationException(AuthenticationException ex) {
        logger.warn("认证失败: {}", ex.getMessage());
        return ApiResponse.error(401, "认证失败: " + ex.getMessage());
    }
    
    /**
     * 处理凭据异常
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public ApiResponse<String> handleBadCredentialsException(BadCredentialsException ex) {
        logger.warn("凭据无效: {}", ex.getMessage());
        return ApiResponse.error(401, "用户名或密码错误");
    }
    
    /**
     * 处理访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public ApiResponse<String> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("访问被拒绝: {}", ex.getMessage());
        return ApiResponse.error(403, "没有权限执行此操作");
    }
    
    /**
     * 处理文件上传大小超出限制异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ApiResponse<String> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        logger.warn("文件上传大小超出限制: {}", ex.getMessage());
        return ApiResponse.error(400, "上传文件大小超出限制");
    }
    
    /**
     * 处理IO异常
     */
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ApiResponse<String> handleIOException(IOException ex) {
        logger.error("IO异常: {}", ex.getMessage(), ex);
        return ApiResponse.error(500, "服务器IO错误: " + ex.getMessage());
    }
    
    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ApiResponse<String> handleException(Exception ex) {
        logger.error("未处理的异常: {}", ex.getMessage(), ex);
        return ApiResponse.error(500, "服务器内部错误: " + ex.getMessage());
    }
} 