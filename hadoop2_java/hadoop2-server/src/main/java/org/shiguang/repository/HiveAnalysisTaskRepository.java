package org.shiguang.repository;

import org.shiguang.entity.HiveAnalysisTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Hive分析任务Repository
 */
@Repository
public interface HiveAnalysisTaskRepository extends JpaRepository<HiveAnalysisTask, String> {
    
    /**
     * 按状态查询任务
     */
    List<HiveAnalysisTask> findByStatus(String status);
    
    /**
     * 按状态和创建者查询任务
     */
    List<HiveAnalysisTask> findByStatusAndCreatedBy(String status, String createdBy);
    
    /**
     * 按分析类型查询任务
     */
    List<HiveAnalysisTask> findByAnalysisType(String analysisType);
    
    /**
     * 按创建者查询任务
     */
    List<HiveAnalysisTask> findByCreatedBy(String createdBy);
    
    /**
     * 按数据库和表查询任务
     */
    List<HiveAnalysisTask> findByDatabaseAndTable(String database, String table);
    
    /**
     * 查询最近的任务（按开始时间降序排序）
     */
    @Query("SELECT t FROM HiveAnalysisTask t ORDER BY t.startTime DESC")
    List<HiveAnalysisTask> findRecentTasks(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 按创建者查询最近的任务
     */
    @Query("SELECT t FROM HiveAnalysisTask t WHERE t.createdBy = :createdBy ORDER BY t.startTime DESC")
    List<HiveAnalysisTask> findRecentTasksByCreatedBy(@Param("createdBy") String createdBy, org.springframework.data.domain.Pageable pageable);
} 