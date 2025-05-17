package org.shiguang.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理系统中抛出的各种异常
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理一般业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusinessException(BusinessException ex) {
        log.error("业务异常: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getMessage());
        response.put("status", ex.getStatus().value());
        response.put("code", ex.getErrorCode());
        
        return new ResponseEntity<>(response, ex.getStatus());
    }
    
    /**
     * 处理JWT相关异常
     */
    @ExceptionHandler({
        ExpiredJwtException.class,
        UnsupportedJwtException.class,
        MalformedJwtException.class,
        SignatureException.class
    })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<Object> handleJwtException(Exception ex) {
        String message = "无效的认证令牌";
        if (ex instanceof ExpiredJwtException) {
            message = "认证令牌已过期";
        }
        
        log.error("JWT异常: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        response.put("status", HttpStatus.UNAUTHORIZED.value());
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * 处理认证和授权异常
     */
    @ExceptionHandler({
        BadCredentialsException.class,
        AuthenticationException.class,
        AccessDeniedException.class
    })
    public ResponseEntity<Object> handleAuthenticationException(Exception ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        String message = "认证失败";
        
        if (ex instanceof AccessDeniedException) {
            status = HttpStatus.FORBIDDEN;
            message = "权限不足";
        } else if (ex instanceof BadCredentialsException) {
            message = "用户名或密码错误";
        }
        
        log.error("认证/授权异常: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        response.put("status", status.value());
        
        return new ResponseEntity<>(response, status);
    }
    
    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        log.error("文件上传大小超限: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "上传文件大小超过限制");
        response.put("status", HttpStatus.BAD_REQUEST.value());
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        response.put("errors", errors);
        response.put("error", "参数验证失败");
        response.put("status", HttpStatus.BAD_REQUEST.value());
        
        log.error("参数验证失败: {}", errors);
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理IO异常
     */
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Object> handleIOException(IOException ex) {
        log.error("IO异常: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "系统处理文件时发生错误");
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 处理所有其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Object> handleAllUncaughtException(Exception ex) {
        log.error("未捕获的异常: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "系统内部错误");
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
} 