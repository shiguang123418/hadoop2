package com.example.websocketdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${websocket.endpoint:/ws}")
    private String websocketEndpoint;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()  // 禁用CSRF以简化WebSocket连接
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            // 允许访问WebSocket端点
            .antMatchers(websocketEndpoint, websocketEndpoint + "/**").permitAll()
            // 允许访问静态资源
            .antMatchers("/", "/index.html", "/css/**", "/js/**", "/*.js", "/*.json", "/*.ico").permitAll()
            // 允许访问SockJS端点
            .antMatchers(websocketEndpoint + "/**/**").permitAll()
            // 所有其他请求需要认证
            .anyRequest().authenticated()
            .and()
            // 仅为测试环境启用HTTP基本认证
            .httpBasic();
    }
} 