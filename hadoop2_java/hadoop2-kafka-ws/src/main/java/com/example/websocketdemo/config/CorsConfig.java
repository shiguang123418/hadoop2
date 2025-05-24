package com.example.websocketdemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Value("${websocket.allowed-origins:*}")
    private String[] allowedOrigins;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 注意：当allowCredentials为true时，不能使用通配符*
        if (allowedOrigins.length == 1 && "*".equals(allowedOrigins[0])) {
            registry.addMapping("/**")
                    .allowedOriginPatterns("*")  // 使用allowedOriginPatterns代替allowedOrigins
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
        } else {
            registry.addMapping("/**")
                    .allowedOrigins(allowedOrigins)
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
        }
    }
    
    // 添加一个CorsFilter作为备用，确保所有请求都能正确处理CORS
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        if (allowedOrigins.length == 1 && "*".equals(allowedOrigins[0])) {
            config.addAllowedOriginPattern("*");
        } else {
            for (String origin : allowedOrigins) {
                config.addAllowedOrigin(origin);
            }
        }
        
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
} 