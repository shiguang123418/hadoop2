package org.shiguang.module.auth.security;

import org.shiguang.module.auth.service.AuthService;
import org.shiguang.module.common.security.SecurityConstants;
import org.springframework.beans.factory.annotation.Autowired;
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
 * JWT Token验证过滤器
 */
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    private final AuthService authService;

    @Autowired
    public JwtTokenFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        try {
            if (token != null) {
                Authentication auth = authService.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取JWT令牌
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(SecurityConstants.HEADER_AUTH);
        
        if (bearerToken != null && bearerToken.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        
        return null;
    }
} 