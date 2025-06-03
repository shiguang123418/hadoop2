package org.shiguang.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.shiguang.entity.User;

import java.util.Date;

/**
 * 用户数据传输对象，用于过滤敏感信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    private Long id;
    private String username;
    private String email;
    private String name;
    private String phone;
    private String avatar;
    private String role;
    private String status;
    private Boolean active;
    private Date createdAt;
    private Date updatedAt;
    private Date lastLoginAt;
    
    /**
     * 从User实体创建UserDTO，过滤敏感字段
     * 
     * @param user 用户实体
     * @return 过滤后的用户DTO
     */
    public static UserDTO fromUser(User user) {
        if (user == null) {
            return null;
        }
        
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setActive(user.getActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setLastLoginAt(user.getLastLoginAt());
        
        // 不包含密码和密码修改相关信息
        
        return dto;
    }
}