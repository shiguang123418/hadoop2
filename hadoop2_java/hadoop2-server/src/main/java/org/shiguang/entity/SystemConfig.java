package org.shiguang.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 系统配置实体类
 * 用于存储系统可配置的参数
 */
@Entity
@Table(name = "system_configs")
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 配置键（唯一）
     */
    @Column(name = "config_key", nullable = false, unique = true)
    private String configKey;

    /**
     * 配置值
     */
    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    /**
     * 配置描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 配置分组
     */
    @Column(name = "config_group")
    private String configGroup;

    /**
     * 值类型：STRING, NUMBER, BOOLEAN, JSON, TEXT
     */
    @Column(name = "value_type")
    private String valueType;

    /**
     * 是否系统级别（系统级别的配置不允许通过界面删除）
     */
    @Column(name = "is_system")
    private boolean system;

    /**
     * 是否可见（不可见的配置不会在界面上显示）
     */
    @Column(name = "is_visible")
    private boolean visible = true;

    /**
     * 排序
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /**
     * 创建时间
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 创建人
     */
    @Column(name = "created_by")
    private String createdBy;

    /**
     * 更新人
     */
    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public SystemConfig() {
    }

    public SystemConfig(String configKey, String configValue, String description, String configGroup, String valueType, boolean system) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
        this.configGroup = configGroup;
        this.valueType = valueType;
        this.system = system;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConfigGroup() {
        return configGroup;
    }

    public void setConfigGroup(String configGroup) {
        this.configGroup = configGroup;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
} 