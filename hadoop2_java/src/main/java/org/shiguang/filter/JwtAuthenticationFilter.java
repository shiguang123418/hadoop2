package org.shiguang.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shiguang.service.AuthService;
import org.shiguang.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JWT认证过滤器
 * 从请求头中提取JWT令牌并验证用户身份
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private AuthService authService;
    
    // 公共路径，不需要认证（没有上下文路径前缀）
    private List<String> publicPaths = Arrays.asList(
        "/auth/", 
        "/weather/cities",
        "/weather/",
        "/production/crops"
    );

    /**
     * 构造函数，注入JWT工具类
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 注入AuthService
     * 使用@Lazy注解避免循环依赖
     */
    @Autowired
    public void setAuthService(@Lazy AuthService authService) {
        this.authService = authService;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // 注意：这里获取的是带有上下文路径的完整URI
        String path = request.getRequestURI();
        // 获取上下文路径
        String contextPath = request.getContextPath();
        log.debug("请求URI: {}, 上下文路径: {}", path, contextPath);
        
        // 跳过预检请求
        if (request.getMethod().equals("OPTIONS")) {
            log.debug("跳过预检请求: {}", path);
            return true;
        }
        
        // 检查请求路径是否在公共路径列表中
        for (String publicPath : publicPaths) {
            // 检查是否包含上下文路径的完整匹配
            if (path.endsWith(publicPath)) {
                log.debug("跳过公共路径(完整匹配): {}", path);
                return true;
            }
            
            // 检查实际路径部分(无上下文)
            String actualPath = path;
            if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
                actualPath = path.substring(contextPath.length());
                if (actualPath.startsWith(publicPath)) {
                    log.debug("跳过公共路径(去除上下文): {} -> {}", path, actualPath);
                    return true;
                }
            }
        }
        
        log.debug("请求需要认证: {}", path);
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 从请求头中获取JWT令牌
            String jwt = getJwtFromRequest(request);

            // 如果JWT有效，则设置用户认证信息
            if (StringUtils.hasText(jwt)) {
                String username = jwtUtil.getUsernameFromToken(jwt);
                
                if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 创建或加载用户详情
                    UserDetails userDetails;
                    if (authService != null) {
                        // 如果AuthService可用，则使用它加载用户
                        userDetails = authService.loadUserByUsername(username);
                    } else {
                        // 否则创建一个简单的用户
                        userDetails = createDefaultUserDetails(username);
                    }
                    
                    // 验证令牌
                    if (jwtUtil.validateToken(jwt, username)) {
                        // 创建认证令牌
                        UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        
                        // 设置认证详情
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        // 设置认证信息
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        log.debug("已为用户 {} 设置安全上下文", username);
                    }
                }
            }
        } catch (Exception e) {
            log.error("无法设置用户认证", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取JWT令牌
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }

    /**
     * 创建默认用户详情
     * 注意：这是一个简化实现，实际应用中应该从数据库中加载用户信息
     */
    private UserDetails createDefaultUserDetails(String username) {
        // 这里应该从数据库中加载用户信息和权限
        // 这只是一个简化实现
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_USER"));
        
        return new User(username, "", authorities);
    }
} 