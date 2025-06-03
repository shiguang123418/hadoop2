package org.shiguang.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应DTO，包含令牌和用户信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    
    /**
     * JWT认证令牌
     */
    private String token;
    
    /**
     * 用户信息（不含敏感字段）
     */
    private UserDTO user;
    
    /**
     * 从登录结果Map创建
     * 
     * @param token JWT令牌
     * @param userDTO 用户DTO
     * @return 登录响应DTO
     */
    public static LoginResponseDTO of(String token, UserDTO userDTO) {
        return new LoginResponseDTO(token, userDTO);
    }
} 