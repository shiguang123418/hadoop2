package org.shiguang.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLoginAt;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date passwordLastChanged;
    
    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = createdAt;
        passwordLastChanged = createdAt;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
} 