package org.shiguang.security.security;

import io.jsonwebtoken.JwtException;
import org.shiguang.security.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT令牌过滤器 - 验证请求中的JWT令牌并设置认证信息
 */
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    @Autowired
    private ApplicationContext applicationContext;
    
    private AuthService authService;

    private AuthService getAuthService() {
        if (authService == null) {
            authService = applicationContext.getBean(AuthService.class);
        }
        return authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        
        try {
            if (token != null) {
                Authentication auth = getAuthService().getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (JwtException e) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("无效的令牌");
            return;
        }
        
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
} 