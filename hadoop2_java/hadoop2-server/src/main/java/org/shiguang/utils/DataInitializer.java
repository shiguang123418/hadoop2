package org.shiguang.utils;

import org.shiguang.repository.UserRepository;
import org.shiguang.module.auth.Constants.SecurityConstants;
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
            user.setName(fullName);
            
            // 确保admin用户获得ROLE_ADMIN角色
            if ("admin".equals(username)) {
                user.setRole(SecurityConstants.ROLE_ADMIN);
            } else {
                user.setRole(roles.get(0));
            }
            
            user.setStatus("active");

            userRepository.save(user);
            System.out.println("已创建用户: " + username);
        } else {
            // 检查并更新现有admin用户的角色
            if ("admin".equals(username)) {
                User adminUser = existingUser.get();
                if (!SecurityConstants.ROLE_ADMIN.equals(adminUser.getRole())) {
                    adminUser.setRole(SecurityConstants.ROLE_ADMIN);
                    userRepository.save(adminUser);
                    System.out.println("已更新admin用户角色为: " + SecurityConstants.ROLE_ADMIN);
                }
            }
        }
    }
} 