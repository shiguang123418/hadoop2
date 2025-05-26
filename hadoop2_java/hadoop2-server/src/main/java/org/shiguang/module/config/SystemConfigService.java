package org.shiguang.module.config;

import org.shiguang.entity.SystemConfig;
import org.shiguang.module.audit.AuditOperation;
import org.shiguang.repository.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置服务
 */
@Service
public class SystemConfigService {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemConfigService.class);

    @Autowired
    private SystemConfigRepository configRepository;
    
    // 内存缓存，用于提高配置访问性能
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    /**
     * 初始化系统默认配置
     */
    @PostConstruct
    @Transactional
    public void initSystemConfigs() {
        logger.info("初始化系统配置...");
        
        // 定义系统默认配置
        List<SystemConfig> defaultConfigs = new ArrayList<>();
        
        // 系统名称配置
        defaultConfigs.add(new SystemConfig(
            "system.name",
            "农业数据可视化平台",
            "系统名称",
            "系统",
            "STRING",
            true
        ));
        
        // 系统版本配置
        defaultConfigs.add(new SystemConfig(
            "system.version",
            "1.0.0",
            "系统版本",
            "系统",
            "STRING",
            true
        ));
        
        // 系统管理员邮箱
        defaultConfigs.add(new SystemConfig(
            "system.admin.email",
            "admin@example.com",
            "系统管理员邮箱",
            "系统",
            "STRING",
            true
        ));
        
        // 是否允许新用户注册
        defaultConfigs.add(new SystemConfig(
            "user.allow.register",
            "true",
            "是否允许新用户注册",
            "用户",
            "BOOLEAN",
            true
        ));
        
        // 默认用户角色
        defaultConfigs.add(new SystemConfig(
            "user.default.role",
            "USER",
            "新用户默认角色",
            "用户",
            "STRING",
            true
        ));
        
        // 文件上传配置
        defaultConfigs.add(new SystemConfig(
            "upload.max.size",
            "10485760", // 10MB
            "上传文件最大大小（字节）",
            "上传",
            "NUMBER",
            true
        ));
        
        defaultConfigs.add(new SystemConfig(
            "upload.allowed.types",
            "jpg,jpeg,png,gif,doc,docx,xls,xlsx,pdf,zip,rar",
            "允许上传的文件类型（逗号分隔）",
            "上传",
            "STRING",
            true
        ));
        
        // 数据可视化配置
        defaultConfigs.add(new SystemConfig(
            "visualization.default.theme",
            "light",
            "可视化默认主题",
            "可视化",
            "STRING",
            true
        ));
        
        defaultConfigs.add(new SystemConfig(
            "visualization.refresh.interval",
            "30000",
            "可视化自动刷新间隔（毫秒）",
            "可视化",
            "NUMBER",
            true
        ));
        
        // 农业数据相关配置
        defaultConfigs.add(new SystemConfig(
            "agriculture.data.source",
            "local",
            "农业数据来源",
            "农业数据",
            "STRING",
            true
        ));
        
        defaultConfigs.add(new SystemConfig(
            "agriculture.default.region",
            "华北",
            "默认农业区域",
            "农业数据",
            "STRING",
            true
        ));
        
        // 循环添加默认配置（如果不存在）
        for (SystemConfig config : defaultConfigs) {
            if (!configRepository.existsByConfigKeyIgnoreCase(config.getConfigKey())) {
                logger.info("添加系统默认配置: {}", config.getConfigKey());
                configRepository.save(config);
            }
        }
        
        // 预热缓存
        refreshCache();
    }
    
    /**
     * 刷新配置缓存
     */
    public void refreshCache() {
        logger.info("刷新系统配置缓存");
        configCache.clear();
        
        List<SystemConfig> configs = configRepository.findAll();
        for (SystemConfig config : configs) {
            configCache.put(config.getConfigKey(), config.getConfigValue());
        }
        
        logger.info("系统配置缓存已刷新，共 {} 项", configCache.size());
    }
    
    /**
     * 获取所有配置
     */
    public List<SystemConfig> getAllConfigs() {
        return configRepository.findAll();
    }
    
    /**
     * 获取所有可见配置
     */
    public List<SystemConfig> getAllVisibleConfigs() {
        return configRepository.findByVisibleTrueOrderBySortOrderAscConfigKeyAsc();
    }
    
    /**
     * 按分组获取配置
     */
    public List<SystemConfig> getConfigsByGroup(String group) {
        return configRepository.findByConfigGroup(group);
    }
    
    /**
     * 按分组获取可见配置
     */
    public List<SystemConfig> getVisibleConfigsByGroup(String group) {
        return configRepository.findByConfigGroupAndVisibleTrueOrderBySortOrderAscConfigKeyAsc(group);
    }
    
    /**
     * 根据ID获取配置
     */
    public Optional<SystemConfig> getConfigById(Long id) {
        return configRepository.findById(id);
    }
    
    /**
     * 根据键获取配置
     */
    public Optional<SystemConfig> getConfigByKey(String key) {
        return configRepository.findByConfigKeyIgnoreCase(key);
    }
    
    /**
     * 获取配置值（字符串）
     */
    public String getConfigValueAsString(String key) {
        // 优先从缓存获取
        if (configCache.containsKey(key)) {
            return configCache.get(key);
        }
        
        // 缓存中不存在，从数据库获取
        Optional<SystemConfig> config = configRepository.findByConfigKeyIgnoreCase(key);
        return config.map(SystemConfig::getConfigValue).orElse(null);
    }
    
    /**
     * 获取配置值（整数）
     */
    public Integer getConfigValueAsInt(String key) {
        String value = getConfigValueAsString(key);
        try {
            return value != null ? Integer.parseInt(value) : null;
        } catch (NumberFormatException e) {
            logger.error("配置值转换为整数失败: {} = {}", key, value, e);
            return null;
        }
    }
    
    /**
     * 获取配置值（长整数）
     */
    public Long getConfigValueAsLong(String key) {
        String value = getConfigValueAsString(key);
        try {
            return value != null ? Long.parseLong(value) : null;
        } catch (NumberFormatException e) {
            logger.error("配置值转换为长整数失败: {} = {}", key, value, e);
            return null;
        }
    }
    
    /**
     * 获取配置值（双精度浮点数）
     */
    public Double getConfigValueAsDouble(String key) {
        String value = getConfigValueAsString(key);
        try {
            return value != null ? Double.parseDouble(value) : null;
        } catch (NumberFormatException e) {
            logger.error("配置值转换为浮点数失败: {} = {}", key, value, e);
            return null;
        }
    }
    
    /**
     * 获取配置值（布尔值）
     */
    public Boolean getConfigValueAsBoolean(String key) {
        String value = getConfigValueAsString(key);
        return value != null ? Boolean.parseBoolean(value) : null;
    }
    
    /**
     * 搜索配置
     */
    public Page<SystemConfig> searchConfigs(
            String configKey,
            String configValue,
            String description,
            String configGroup,
            String valueType,
            Boolean system,
            Boolean visible,
            Pageable pageable) {
        
        return configRepository.searchConfigs(
            configKey,
            configValue,
            description,
            configGroup,
            valueType,
            system,
            visible,
            pageable
        );
    }
    
    /**
     * 创建配置
     */
    @Transactional
    @AuditOperation(operation = "创建系统配置", operationType = "CREATE", resourceType = "SYSTEM_CONFIG")
    public SystemConfig createConfig(SystemConfig config, String username) {
        // 检查键是否已存在
        if (configRepository.existsByConfigKeyIgnoreCase(config.getConfigKey())) {
            throw new IllegalArgumentException("配置键已存在: " + config.getConfigKey());
        }
        
        config.setCreatedBy(username);
        SystemConfig savedConfig = configRepository.save(config);
        
        // 更新缓存
        configCache.put(savedConfig.getConfigKey(), savedConfig.getConfigValue());
        
        return savedConfig;
    }
    
    /**
     * 更新配置
     */
    @Transactional
    @AuditOperation(operation = "更新系统配置", operationType = "UPDATE", resourceType = "SYSTEM_CONFIG", resourceIdIndex = 0)
    public SystemConfig updateConfig(Long id, SystemConfig configDetails, String username) {
        return configRepository.findById(id)
            .map(existingConfig -> {
                // 检查键是否已存在（排除自身）
                if (!existingConfig.getConfigKey().equalsIgnoreCase(configDetails.getConfigKey()) &&
                    configRepository.existsByConfigKeyIgnoreCaseAndIdNot(configDetails.getConfigKey(), id)) {
                    throw new IllegalArgumentException("配置键已存在: " + configDetails.getConfigKey());
                }
                
                // 系统级配置，只允许修改配置值、描述和排序
                if (existingConfig.isSystem()) {
                    existingConfig.setConfigValue(configDetails.getConfigValue());
                    existingConfig.setDescription(configDetails.getDescription());
                    existingConfig.setSortOrder(configDetails.getSortOrder());
                } else {
                    // 非系统级配置，可以修改所有字段
                    existingConfig.setConfigKey(configDetails.getConfigKey());
                    existingConfig.setConfigValue(configDetails.getConfigValue());
                    existingConfig.setDescription(configDetails.getDescription());
                    existingConfig.setConfigGroup(configDetails.getConfigGroup());
                    existingConfig.setValueType(configDetails.getValueType());
                    existingConfig.setVisible(configDetails.isVisible());
                    existingConfig.setSortOrder(configDetails.getSortOrder());
                }
                
                existingConfig.setUpdatedBy(username);
                SystemConfig updatedConfig = configRepository.save(existingConfig);
                
                // 更新缓存
                configCache.put(updatedConfig.getConfigKey(), updatedConfig.getConfigValue());
                
                return updatedConfig;
            })
            .orElseThrow(() -> new IllegalArgumentException("配置不存在: " + id));
    }
    
    /**
     * 更新配置值
     */
    @Transactional
    @AuditOperation(operation = "更新系统配置值", operationType = "UPDATE", resourceType = "SYSTEM_CONFIG", resourceIdIndex = 0)
    public SystemConfig updateConfigValue(Long id, String newValue, String username) {
        return configRepository.findById(id)
            .map(existingConfig -> {
                existingConfig.setConfigValue(newValue);
                existingConfig.setUpdatedBy(username);
                SystemConfig updatedConfig = configRepository.save(existingConfig);
                
                // 更新缓存
                configCache.put(updatedConfig.getConfigKey(), updatedConfig.getConfigValue());
                
                return updatedConfig;
            })
            .orElseThrow(() -> new IllegalArgumentException("配置不存在: " + id));
    }
    
    /**
     * 更新配置值（通过键）
     */
    @Transactional
    @AuditOperation(operation = "更新系统配置值", operationType = "UPDATE", resourceType = "SYSTEM_CONFIG")
    public SystemConfig updateConfigValueByKey(String key, String newValue, String username) {
        return configRepository.findByConfigKeyIgnoreCase(key)
            .map(existingConfig -> {
                existingConfig.setConfigValue(newValue);
                existingConfig.setUpdatedBy(username);
                SystemConfig updatedConfig = configRepository.save(existingConfig);
                
                // 更新缓存
                configCache.put(updatedConfig.getConfigKey(), updatedConfig.getConfigValue());
                
                return updatedConfig;
            })
            .orElseThrow(() -> new IllegalArgumentException("配置不存在: " + key));
    }
    
    /**
     * 删除配置
     * 只能删除非系统级别的配置
     */
    @Transactional
    @AuditOperation(operation = "删除系统配置", operationType = "DELETE", resourceType = "SYSTEM_CONFIG", resourceIdIndex = 0)
    public void deleteConfig(Long id) {
        Optional<SystemConfig> config = configRepository.findById(id);
        
        if (config.isPresent()) {
            // 检查是否为系统级配置
            if (config.get().isSystem()) {
                throw new IllegalArgumentException("不能删除系统级配置: " + config.get().getConfigKey());
            }
            
            // 从缓存中移除
            configCache.remove(config.get().getConfigKey());
            
            // 从数据库中删除
            configRepository.deleteById(id);
        }
    }
    
    /**
     * 批量更新配置
     */
    @Transactional
    @AuditOperation(operation = "批量更新系统配置", operationType = "UPDATE", resourceType = "SYSTEM_CONFIG")
    public List<SystemConfig> batchUpdateConfigs(Map<String, String> configMap, String username) {
        List<SystemConfig> updatedConfigs = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            configRepository.findByConfigKeyIgnoreCase(key).ifPresent(config -> {
                config.setConfigValue(value);
                config.setUpdatedBy(username);
                SystemConfig updatedConfig = configRepository.save(config);
                
                // 更新缓存
                configCache.put(key, value);
                
                updatedConfigs.add(updatedConfig);
            });
        }
        
        return updatedConfigs;
    }
} 