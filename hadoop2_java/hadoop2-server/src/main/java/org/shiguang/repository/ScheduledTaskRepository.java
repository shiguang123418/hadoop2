package org.shiguang.repository;

import org.shiguang.entity.ScheduledTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 定时任务仓库
 */
@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, Long> {
    
    /**
     * 根据名称查找定时任务
     */
    Optional<ScheduledTask> findByName(String name);
    
    /**
     * 检查名称是否已存在（除了指定ID外）
     */
    boolean existsByNameAndIdNot(String name, Long id);
    
    /**
     * 查找所有启用的定时任务
     */
    List<ScheduledTask> findByEnabledTrue();
    
    /**
     * 查找所有启用或禁用的定时任务
     */
    List<ScheduledTask> findByEnabled(boolean enabled);
    
    /**
     * 按任务类名查找定时任务
     */
    Page<ScheduledTask> findByJobClassContaining(String jobClass, Pageable pageable);
    
    /**
     * 高级搜索
     */
    @Query("SELECT t FROM ScheduledTask t WHERE " +
           "(:name IS NULL OR t.name LIKE %:name%) AND " +
           "(:description IS NULL OR t.description LIKE %:description%) AND " +
           "(:enabled IS NULL OR t.enabled = :enabled) AND " +
           "(:jobClass IS NULL OR t.jobClass LIKE %:jobClass%)")
    Page<ScheduledTask> searchTasks(
            @Param("name") String name,
            @Param("description") String description,
            @Param("enabled") Boolean enabled,
            @Param("jobClass") String jobClass,
            Pageable pageable);
} 