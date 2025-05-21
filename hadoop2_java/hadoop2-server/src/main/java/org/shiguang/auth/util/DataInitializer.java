package org.shiguang.auth.util;

import org.shiguang.common.repository.UserRepository;
import org.shiguang.common.security.SecurityConstants;
import org.shiguang.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * 初始化测试数据
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 创建默认管理员用户
        createUserIfNotExists("admin", "082415", "admin@example.com", "管理员",
                Arrays.asList(SecurityConstants.ROLE_ADMIN, SecurityConstants.ROLE_USER));

        // 创建默认普通用户
        createUserIfNotExists("user", "082415", "user@example.com", "普通用户",
                Arrays.asList(SecurityConstants.ROLE_USER));
                
        // 创建测试用户 y1/123456
        createUserIfNotExists("y1", "082415", "y1@example.com", "测试用户",
                Arrays.asList(SecurityConstants.ROLE_USER));
    }

    private void createUserIfNotExists(String username, String password, String email, String fullName, 
                                      java.util.List<String> roles) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        
        if (!existingUser.isPresent()) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setEmail(email);
            user.setFullName(fullName);
            user.setRoles(roles);
            user.setActive(true);

            userRepository.save(user);
            System.out.println("已创建用户: " + username);
        }
    }
} 