package org.shiguang.module.auth.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.shiguang.entity.User;
import org.shiguang.module.auth.service.AuthService;
import org.shiguang.module.common.security.SecurityConstants;
import org.shiguang.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 认证服务实现类
 */
@Service
public class AuthServiceImpl implements AuthService, UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Value("${security.jwt.token.secret-key}")
    private String secretKey;

    @Value("${security.jwt.token.expire-length}")
    private long validityInMilliseconds;

    private SecretKey key;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    protected void init() {
        // 使用JJWT 0.11.x的API创建密钥
        key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    @Override
    public Map<String, Object> login(String username, String password) {
        try {
            // 输入验证
            if (username == null || username.trim().isEmpty()) {
                logger.warn("登录失败: 用户名为空");
                throw new IllegalArgumentException("用户名不能为空");
            }
            
            if (password == null || password.trim().isEmpty()) {
                logger.warn("登录失败: 密码为空");
                throw new IllegalArgumentException("密码不能为空");
            }
            
            logger.info("尝试登录: 用户名={}", username);
            
            // 查找用户
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (!userOptional.isPresent()) {
                logger.warn("登录失败: 用户名不存在 - {}", username);
                throw new UsernameNotFoundException("用户名或密码错误");
            }
            
            User user = userOptional.get();
            
            // 检查用户状态
            if (!"active".equals(user.getStatus())) {
                logger.warn("登录失败: 用户账号已被禁用或锁定 - {}, 状态={}", username, user.getStatus());
                throw new IllegalStateException("账号已被禁用或锁定");
            }
            
            // 验证密码
            if (!passwordEncoder.matches(password, user.getPassword())) {
                logger.warn("登录失败: 密码错误 - {}", username);
                throw new BadCredentialsException("用户名或密码错误");
            }

            // 打印用户信息
            logger.info("用户登录成功: {}, ID: {}, 角色: {}, 头像: {}", 
                        username, 
                        user.getId(), 
                        user.getRole(),
                        user.getAvatar() != null ? user.getAvatar() : "无头像");

            // 更新最后登录时间
            user.setLastLoginAt(new Date());
            userRepository.save(user);

            // 生成令牌
            String token = createToken(username);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            
            // 复制用户信息，排除敏感字段
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("name", user.getName());
            
            // 确保头像URL正确返回
            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                userInfo.put("avatar", user.getAvatar());
                logger.info("返回用户头像URL: {}", user.getAvatar());
            } else {
                userInfo.put("avatar", null);
                logger.info("用户没有头像");
            }
            
            // 确保roles不为null
            String role = user.getRole();
            if (role != null && !role.isEmpty()) {
                // 处理ROLE_前缀
                if (!role.startsWith("ROLE_")) {
                    role = "ROLE_" + role.toUpperCase();
                }
                userInfo.put("roles", Collections.singletonList(role));
            } else {
                // 默认为普通用户角色
                userInfo.put("roles", Collections.singletonList("ROLE_USER"));
            }
            
            userInfo.put("active", "active".equals(user.getStatus()));
            
            response.put("user", userInfo);
            
            // 打印完整响应，但排除敏感信息
            logger.info("返回登录响应，用户: {}, 角色: {}, 有头像: {}", 
                        username, 
                        role, 
                        user.getAvatar() != null);

            return response;
        } catch (UsernameNotFoundException | BadCredentialsException e) {
            // 用户名或密码错误的情况，使用一致的错误消息
            logger.warn("登录认证失败: {}", e.getMessage());
            throw new BadCredentialsException("用户名或密码错误");
        } catch (IllegalStateException e) {
            // 账号状态问题
            logger.warn("账号状态异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // 其他未预期的异常，记录详细信息但不向客户端暴露
            logger.error("登录过程发生异常: {}", e.getMessage(), e);
            throw new RuntimeException("登录失败，请稍后再试");
        }
    }

    @Override
    public User register(User user) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 设置默认角色
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole(SecurityConstants.ROLE_USER);
        }

        // 保存用户
        return userRepository.save(user);
    }

    @Override
    public String createToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        Claims claims = Jwts.claims().setSubject(username);
        // 确保角色带有ROLE_前缀
        String role = user.getRole();
        if (role != null && !role.isEmpty() && !role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }
        claims.put("roles", Collections.singletonList(role));

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)  // 更新的API调用
                .compact();
    }

    @Override
    public Authentication getAuthentication(String token) {
        // 更新的JJWT解析API
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String username = claims.getSubject();
        List<String> roles = claims.get("roles", List.class);

        UserDetails userDetails = loadUserByUsername(username);

        return new UsernamePasswordAuthenticationToken(
                userDetails, "", userDetails.getAuthorities());
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.debug("getCurrentUser: 用户未认证或认证上下文为空");
            return null;
        }

        Object principal = authentication.getPrincipal();
        String username;

        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        logger.debug("getCurrentUser: 从认证上下文获取用户名: {}", username);
        
        User user = userRepository.findByUsername(username).orElse(null);
        
        if (user != null) {
            logger.debug("getCurrentUser: 找到用户 {}, ID: {}, 头像: {}", 
                        username, 
                        user.getId(), 
                        user.getAvatar() != null ? user.getAvatar() : "无头像");
        } else {
            logger.warn("getCurrentUser: 在数据库中未找到用户 {}", username);
        }
        
        return user;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("未找到用户: " + username));

        // 确保角色带有ROLE_前缀
        String role = user.getRole();
        if (role != null && !role.isEmpty() && !role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                "active".equals(user.getStatus()),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }
    
    @Override
    public boolean validatePassword(String username, String password) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }
        return passwordEncoder.matches(password, user.getPassword());
    }
    
    @Override
    public boolean hasRole(String role) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        
        // 获取用户角色
        String userRole = currentUser.getRole();
        if (userRole == null || userRole.isEmpty()) {
            return false;
        }
        
        // 处理角色前缀，兼容不同格式的角色表示
        return userRole.equalsIgnoreCase(role) || 
               userRole.equalsIgnoreCase("ROLE_" + role) ||
               role.equalsIgnoreCase("ROLE_" + userRole);
    }
} 