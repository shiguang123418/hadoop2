package org.shiguang.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shiguang.entity.User;
import org.shiguang.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化配置，用于创建默认用户
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 创建默认管理员和测试用户
        if (userRepository.count() == 0) {
            log.info("数据库中没有用户，正在创建默认用户...");
            createDefaultUsers();
        } else {
            log.info("数据库中已有用户，跳过默认用户创建");
            
            // 确保admin用户存在
            if (!userRepository.existsByUsername("admin")) {
                log.info("创建管理员用户: admin");
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .email("admin@example.com")
                        .fullName("系统管理员")
                        .role("ADMIN")
                        .build();
                userRepository.save(admin);
            }
            if (!userRepository.existsByUsername("y1")) {
                User y1 = User.builder()
                        .username("y1")
                        .password(passwordEncoder.encode("082415"))
                        .email("y1@example.com")
                        .fullName("管理员")
                        .role("ADMIN")
                        .build();
                userRepository.save(y1);
            }
        }
    }

    /**
     * 创建默认用户
     */
    private void createDefaultUsers() {
        // 创建管理员用户
        User admin = new User();
        admin.setUsername("y1");
        admin.setPassword(passwordEncoder.encode("082415"));
        admin.setEmail("admin@example.com");
        admin.setFullName("系统管理员");
        admin.setRole("ADMIN");
        userRepository.save(admin);
        log.info("创建管理员用户: {}", admin.getUsername());

        // 创建普通用户
        User user = new User();
        user.setUsername("user");
        user.setPassword(passwordEncoder.encode("user123"));
        user.setEmail("user@example.com");
        user.setFullName("测试用户");
        user.setRole("USER");
        userRepository.save(user);
        log.info("创建普通用户: {}", user.getUsername());
    }
} 