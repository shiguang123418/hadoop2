package org.shiguang.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Hive分析任务实体类
 * 用于存储Hive分析任务的结果和状态
 */
@Entity
@Table(name = "hive_analysis_tasks")
public class HiveAnalysisTask {

    @Id
    @Column(length = 36)
    private String taskId;

    @Column(nullable = false, length = 50)
    private String analysisType;

    @Column(name = "`database`", nullable = false, length = 100)
    private String database;

    @Column(name = "`table`", nullable = false, length = 100)
    private String table;

    @Column(columnDefinition = "TEXT")
    private String parameters;

    @Column(nullable = false, length = 20)
    private String status;

    @Column
    private Integer progress;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(columnDefinition = "TEXT")
    private String resultData;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    // 构造函数
    public HiveAnalysisTask() {
    }

    public HiveAnalysisTask(String taskId, String analysisType, String database, String table, String parameters, String status, LocalDateTime startTime) {
        this.taskId = taskId;
        this.analysisType = analysisType;
        this.database = database;
        this.table = table;
        this.parameters = parameters;
        this.status = status;
        this.startTime = startTime;
        this.progress = 0;
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getResultData() {
        return resultData;
    }

    public void setResultData(String resultData) {
        this.resultData = resultData;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
} 