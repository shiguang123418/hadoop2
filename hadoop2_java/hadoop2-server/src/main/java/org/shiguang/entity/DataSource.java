package org.shiguang.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "data_sources")
public class DataSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;            // 数据源名称

    @Column(nullable = false, length = 50)
    private String type;            // 数据源类型（如：传感器、气象站、市场数据等）

    @Column(length = 500)
    private String description;     // 数据源描述

    @Column(columnDefinition = "TEXT")
    private String connectionInfo;  // 连接信息（JSON格式存储）

    @Column(nullable = false, length = 20)
    private String status;          // 数据源状态（ACTIVE, INACTIVE, ERROR）

    @Column(nullable = false)
    private LocalDateTime createTime;

    @Column(nullable = false)
    private LocalDateTime updateTime;

    @Column(length = 50)
    private String createBy;        // 创建人

    @Column(length = 50)
    private String updateBy;        // 更新人
} 