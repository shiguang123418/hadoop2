package org.shiguang.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    private String email;
    
    private String name;
    
    private String phone;
    
    @Column(name = "avatar")
    private String avatar;
    
    @Column(name = "role")
    private String role = "user";
    
    @Column(name = "status")
    private String status = "active";
    
    @Column(name = "active")
    private Boolean active = true;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLoginAt;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date passwordLastChanged;
    
    // 临时存储字段，不持久化到数据库
    @Transient
    private String passwordCopy;
    
    @Transient
    private Date passwordLastChangedCopy;
    
    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = createdAt;
        passwordLastChanged = createdAt;
    }
    
    @PreUpdate
    protected void onUpdate() {
        // 只更新updatedAt字段，不动其他字段
        updatedAt = new Date();
        
        // 防止密码被框架自动清空的保护逻辑
        if (passwordCopy != null && (password == null || password.isEmpty())) {
            log.warn("实体更新过程中发现密码字段被清空，恢复为原密码");
            password = passwordCopy;
            passwordLastChanged = passwordLastChangedCopy;
        }
    }
    
    /**
     * 在实体被加载后记录密码副本，防止后续操作中被清空
     */
    @PostLoad
    protected void onLoad() {
        if (password != null) {
            passwordCopy = password;
            passwordLastChangedCopy = passwordLastChanged;
        }
    }
} 