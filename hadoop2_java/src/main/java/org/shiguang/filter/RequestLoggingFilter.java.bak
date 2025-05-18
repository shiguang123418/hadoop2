package org.shiguang.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求日志过滤器，用于记录所有请求和响应的细节
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 记录请求详情
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        String method = request.getMethod();
        Map<String, String> headers = getRequestHeaders(request);
        
        log.info("收到请求: {} {} {}", method, requestURI, queryString != null ? "?" + queryString : "");
        log.debug("请求头: {}", headers);
        
        // 执行过滤器链
        long startTime = System.currentTimeMillis();
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 记录响应详情
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            
            log.info("返回响应: {} {} - {} ({} ms)", method, requestURI, status, duration);
        }
    }
    
    /**
     * 获取请求头
     */
    private Map<String, String> getRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                
                // 敏感头信息处理
                if ("authorization".equalsIgnoreCase(headerName) && headerValue != null && !headerValue.isEmpty()) {
                    if (headerValue.toLowerCase().startsWith("bearer ")) {
                        headerValue = "Bearer ...";
                    } else {
                        headerValue = "...";
                    }
                }
                
                headers.put(headerName, headerValue);
            }
        }
        
        return headers;
    }
} 