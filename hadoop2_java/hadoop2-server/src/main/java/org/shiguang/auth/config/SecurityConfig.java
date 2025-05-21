package org.shiguang.auth.config;

import org.shiguang.common.config.BaseSecurityConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.beans.factory.annotation.Value;

/**
 * 服务器模块特定的安全配置
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends BaseSecurityConfig {
    
    @Value("${debug:false}")
    private boolean debugMode;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 先配置特定的规则
        http.authorizeRequests()
            .antMatchers("/hdfs/**", "/hive/**").permitAll();
        
        // 然后调用父类的配置方法来完成配置
        super.configure(http);
        
        if (debugMode) {
            http.headers().frameOptions().disable();
        }
    }
} 