package org.shiguang.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shiguang.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");
        
        log.info("用户尝试登录: {}", username);
        
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "用户名和密码不能为空"
            ));
        }
        
        Map<String, Object> result = authService.login(username, password);
        
        log.info("用户登录成功: {}", username);
        
        return ResponseEntity.ok(result);
    }
} 