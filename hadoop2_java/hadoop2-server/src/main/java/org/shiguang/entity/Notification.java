package org.shiguang.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 系统通知消息实体类
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 通知标题
     */
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * 通知内容
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 通知类型: INFO, WARNING, ERROR, SUCCESS
     */
    @Column(name = "type", nullable = false)
    private String type;

    /**
     * 通知级别: LOW, MEDIUM, HIGH, URGENT
     */
    @Column(name = "level", nullable = false)
    private String level;

    /**
     * 通知来源/模块
     */
    @Column(name = "source")
    private String source;

    /**
     * 接收用户ID
     * 如果为null，表示系统通知(发送给所有用户)
     */
    @Column(name = "user_id")
    private Long userId;
    
    /**
     * 接收用户名称(冗余字段，便于查询展示)
     */
    @Column(name = "username")
    private String username;

    /**
     * 是否已读
     */
    @Column(name = "is_read")
    private boolean read = false;

    /**
     * 阅读时间
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 是否需要确认
     */
    @Column(name = "need_confirm")
    private boolean needConfirm = false;

    /**
     * 是否已确认
     */
    @Column(name = "is_confirmed")
    private boolean confirmed = false;

    /**
     * 确认时间
     */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /**
     * 创建时间
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 是否已删除（逻辑删除）
     */
    @Column(name = "is_deleted")
    private boolean deleted = false;
    
    /**
     * 相关资源类型
     */
    @Column(name = "resource_type")
    private String resourceType;
    
    /**
     * 相关资源ID
     */
    @Column(name = "resource_id")
    private String resourceId;
    
    /**
     * 相关链接
     */
    @Column(name = "link")
    private String link;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public Notification() {
    }

    public Notification(String title, String content, String type, String level, String source) {
        this.title = title;
        this.content = content;
        this.type = type;
        this.level = level;
        this.source = source;
    }
    
    public Notification(String title, String content, String type, String level, String source, Long userId) {
        this.title = title;
        this.content = content;
        this.type = type;
        this.level = level;
        this.source = source;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public boolean isNeedConfirm() {
        return needConfirm;
    }

    public void setNeedConfirm(boolean needConfirm) {
        this.needConfirm = needConfirm;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
    
    public String getResourceId() {
        return resourceId;
    }
    
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
    
    public String getLink() {
        return link;
    }
    
    public void setLink(String link) {
        this.link = link;
    }
} 