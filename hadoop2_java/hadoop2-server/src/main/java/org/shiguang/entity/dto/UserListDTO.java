package org.shiguang.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.shiguang.entity.User;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户列表数据传输对象，用于列表展示，包含最少必要信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserListDTO {
    
    private Long id;
    private String username;
    private String name;
    private String email;
    private String avatar;
    private String role;
    private String status;
    private Boolean active;
    private Date lastLoginAt;
    private Date createdAt;
    
    /**
     * 从User实体创建UserListDTO
     * @param user 用户实体
     * @return 用户列表DTO
     */
    public static UserListDTO fromUser(User user) {
        if (user == null) {
            return null;
        }
        
        UserListDTO dto = new UserListDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setActive(user.getActive());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setCreatedAt(user.getCreatedAt());
        
        return dto;
    }
    
    /**
     * 将用户列表转换为DTO列表
     * @param users 用户列表
     * @return DTO列表
     */
    public static List<UserListDTO> fromUserList(List<User> users) {
        if (users == null) {
            return null;
        }
        
        return users.stream()
                .map(UserListDTO::fromUser)
                .collect(Collectors.toList());
    }
} 