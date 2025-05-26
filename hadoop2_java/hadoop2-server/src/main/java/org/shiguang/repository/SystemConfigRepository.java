package org.shiguang.repository;

import org.shiguang.entity.SystemConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置存储库
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    
    /**
     * 根据配置键查询配置（忽略大小写）
     */
    Optional<SystemConfig> findByConfigKeyIgnoreCase(String configKey);
    
    /**
     * 检查配置键是否存在
     */
    boolean existsByConfigKeyIgnoreCase(String configKey);
    
    /**
     * 检查配置键是否存在（排除指定ID）
     */
    boolean existsByConfigKeyIgnoreCaseAndIdNot(String configKey, Long id);
    
    /**
     * 根据配置分组查询配置
     */
    List<SystemConfig> findByConfigGroup(String configGroup);
    
    /**
     * 查询可见的配置
     */
    List<SystemConfig> findByVisibleTrueOrderBySortOrderAscConfigKeyAsc();
    
    /**
     * 根据配置分组查询可见的配置
     */
    List<SystemConfig> findByConfigGroupAndVisibleTrueOrderBySortOrderAscConfigKeyAsc(String configGroup);
    
    /**
     * 高级搜索
     */
    @Query("SELECT c FROM SystemConfig c WHERE " +
           "(:configKey IS NULL OR c.configKey LIKE %:configKey%) AND " +
           "(:configValue IS NULL OR c.configValue LIKE %:configValue%) AND " +
           "(:description IS NULL OR c.description LIKE %:description%) AND " +
           "(:configGroup IS NULL OR c.configGroup = :configGroup) AND " +
           "(:valueType IS NULL OR c.valueType = :valueType) AND " +
           "(:system IS NULL OR c.system = :system) AND " +
           "(:visible IS NULL OR c.visible = :visible)")
    Page<SystemConfig> searchConfigs(
        @Param("configKey") String configKey,
        @Param("configValue") String configValue,
        @Param("description") String description,
        @Param("configGroup") String configGroup,
        @Param("valueType") String valueType,
        @Param("system") Boolean system,
        @Param("visible") Boolean visible,
        Pageable pageable);
    
    /**
     * 删除非系统配置
     */
    void deleteBySystemFalseAndId(Long id);
} 