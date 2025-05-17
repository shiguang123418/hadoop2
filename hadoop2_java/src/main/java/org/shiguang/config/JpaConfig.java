package org.shiguang.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "org.shiguang.entity")
@EnableJpaRepositories(basePackages = "org.shiguang.repository")
public class JpaConfig {
    // JPA配置类，明确指定实体类和仓库接口的包路径
} 