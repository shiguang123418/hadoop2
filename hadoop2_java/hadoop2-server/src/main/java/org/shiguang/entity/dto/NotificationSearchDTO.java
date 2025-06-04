package org.shiguang.entity.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知搜索DTO
 */
public class NotificationSearchDTO {

    /**
     * 用户ID（为null时表示查询管理员可查看的所有通知）
     */
    private Long userId;
    
    /**
     * 通知类型：INFO, WARNING, ERROR, SUCCESS
     */
    private String type;
    
    /**
     * 通知级别：LOW, MEDIUM, HIGH, URGENT
     */
    private String level;
    
    /**
     * 通知来源/模块
     */
    private String source;
    
    /**
     * 是否已读
     */
    private Boolean read;
    
    /**
     * 是否需要确认
     */
    private Boolean needConfirm;
    
    /**
     * 是否已确认
     */
    private Boolean confirmed;
    
    /**
     * 起始日期
     */
    private LocalDateTime startDate;
    
    /**
     * 结束日期
     */
    private LocalDateTime endDate;
    
    /**
     * 关键字搜索（标题或内容）
     */
    private String keyword;
    
    /**
     * 资源类型
     */
    private String resourceType;
    
    /**
     * 通知ID列表，用于批量操作
     */
    private List<Long> ids;
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public Boolean getNeedConfirm() {
        return needConfirm;
    }

    public void setNeedConfirm(Boolean needConfirm) {
        this.needConfirm = needConfirm;
    }

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
} 