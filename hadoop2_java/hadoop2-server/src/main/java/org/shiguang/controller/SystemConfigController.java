package org.shiguang.controller;

import org.shiguang.entity.SystemConfig;
import org.shiguang.entity.dto.ApiResponse;
import org.shiguang.module.audit.AuditOperation;
import org.shiguang.module.auth.service.AuthService;
import org.shiguang.module.config.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 系统配置控制器
 */
@RestController
@RequestMapping("/api/system/configs")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class SystemConfigController {

    @Autowired
    private SystemConfigService configService;
    
    @Autowired
    private AuthService authService;
    
    /**
     * 获取所有配置
     */
    @GetMapping
    @AuditOperation(operation = "查询所有系统配置", operationType = "QUERY", resourceType = "SYSTEM_CONFIG")
    public ResponseEntity<ApiResponse<List<SystemConfig>>> getAllConfigs() {
        List<SystemConfig> configs = configService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.success("获取系统配置成功", configs));
    }
    
    /**
     * 获取所有可见配置（无需管理员权限）
     */
    @GetMapping("/visible")
    public ResponseEntity<ApiResponse<List<SystemConfig>>> getVisibleConfigs() {
        List<SystemConfig> configs = configService.getAllVisibleConfigs();
        return ResponseEntity.ok(ApiResponse.success("获取可见系统配置成功", configs));
    }
    
    /**
     * 分页查询配置
     */
    @GetMapping("/page")
    @AuditOperation(operation = "分页查询系统配置", operationType = "QUERY", resourceType = "SYSTEM_CONFIG")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfigsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "configKey") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        
        // 创建分页请求
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        // 执行查询
        Page<SystemConfig> configsPage = configService.searchConfigs(null, null, null, null, null, null, null, pageable);
        
        // 构造返回结果
        Map<String, Object> response = new HashMap<>();
        response.put("content", configsPage.getContent());
        response.put("totalElements", configsPage.getTotalElements());
        response.put("totalPages", configsPage.getTotalPages());
        response.put("currentPage", configsPage.getNumber());
        
        return ResponseEntity.ok(ApiResponse.success("获取系统配置成功", response));
    }
    
    /**
     * 按分组获取配置
     */
    @GetMapping("/group/{group}")
    @AuditOperation(operation = "按分组查询系统配置", operationType = "QUERY", resourceType = "SYSTEM_CONFIG")
    public ResponseEntity<ApiResponse<List<SystemConfig>>> getConfigsByGroup(@PathVariable String group) {
        List<SystemConfig> configs = configService.getConfigsByGroup(group);
        return ResponseEntity.ok(ApiResponse.success("获取系统配置成功", configs));
    }
    
    /**
     * 按分组获取可见配置（无需管理员权限）
     */
    @GetMapping("/group/{group}/visible")
    public ResponseEntity<ApiResponse<List<SystemConfig>>> getVisibleConfigsByGroup(@PathVariable String group) {
        List<SystemConfig> configs = configService.getVisibleConfigsByGroup(group);
        return ResponseEntity.ok(ApiResponse.success("获取可见系统配置成功", configs));
    }
    
    /**
     * 按ID获取配置
     */
    @GetMapping("/{id}")
    @AuditOperation(operation = "查询系统配置详情", operationType = "QUERY", resourceType = "SYSTEM_CONFIG", resourceIdIndex = 0)
    public ResponseEntity<ApiResponse<SystemConfig>> getConfigById(@PathVariable Long id) {
        Optional<SystemConfig> config = configService.getConfigById(id);
        return config.map(value -> ResponseEntity.ok(ApiResponse.success("获取系统配置成功", value)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error(404, "系统配置不存在")));
    }
    
    /**
     * 按键获取配置
     */
    @GetMapping("/key/{key}")
    @AuditOperation(operation = "按键查询系统配置", operationType = "QUERY", resourceType = "SYSTEM_CONFIG")
    public ResponseEntity<ApiResponse<SystemConfig>> getConfigByKey(@PathVariable String key) {
        Optional<SystemConfig> config = configService.getConfigByKey(key);
        return config.map(value -> ResponseEntity.ok(ApiResponse.success("获取系统配置成功", value)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error(404, "系统配置不存在")));
    }
    
    /**
     * 获取配置值（无需管理员权限）
     */
    @GetMapping("/value/{key}")
    public ResponseEntity<ApiResponse<String>> getConfigValue(@PathVariable String key) {
        String value = configService.getConfigValueAsString(key);
        
        if (value != null) {
            return ResponseEntity.ok(ApiResponse.success("获取配置值成功", value));
        } else {
            return ResponseEntity.ok(ApiResponse.error(404, "配置不存在"));
        }
    }
    
    /**
     * 高级搜索
     */
    @GetMapping("/search")
    @AuditOperation(operation = "搜索系统配置", operationType = "QUERY", resourceType = "SYSTEM_CONFIG")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchConfigs(
            @RequestParam(required = false) String configKey,
            @RequestParam(required = false) String configValue,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String configGroup,
            @RequestParam(required = false) String valueType,
            @RequestParam(required = false) Boolean system,
            @RequestParam(required = false) Boolean visible,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "configKey") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        
        // 创建分页请求
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        // 执行搜索
        Page<SystemConfig> configsPage = configService.searchConfigs(
            configKey, configValue, description, configGroup, valueType, system, visible, pageable);
        
        // 构造返回结果
        Map<String, Object> response = new HashMap<>();
        response.put("content", configsPage.getContent());
        response.put("totalElements", configsPage.getTotalElements());
        response.put("totalPages", configsPage.getTotalPages());
        response.put("currentPage", configsPage.getNumber());
        
        return ResponseEntity.ok(ApiResponse.success("搜索系统配置成功", response));
    }
    
    /**
     * 创建配置
     */
    @PostMapping
    @AuditOperation(operation = "创建系统配置", operationType = "CREATE", resourceType = "SYSTEM_CONFIG")
    public ResponseEntity<ApiResponse<SystemConfig>> createConfig(@RequestBody SystemConfig config) {
        try {
            String username = authService.getCurrentUser().getUsername();
            SystemConfig createdConfig = configService.createConfig(config, username);
            return ResponseEntity.ok(ApiResponse.success("创建系统配置成功", createdConfig));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "创建系统配置失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新配置
     */
    @PutMapping("/{id}")
    @AuditOperation(operation = "更新系统配置", operationType = "UPDATE", resourceType = "SYSTEM_CONFIG", resourceIdIndex = 0)
    public ResponseEntity<ApiResponse<SystemConfig>> updateConfig(@PathVariable Long id, @RequestBody SystemConfig config) {
        try {
            String username = authService.getCurrentUser().getUsername();
            SystemConfig updatedConfig = configService.updateConfig(id, config, username);
            return ResponseEntity.ok(ApiResponse.success("更新系统配置成功", updatedConfig));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "更新系统配置失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新配置值
     */
    @PutMapping("/{id}/value")
    @AuditOperation(operation = "更新系统配置值", operationType = "UPDATE", resourceType = "SYSTEM_CONFIG", resourceIdIndex = 0)
    public ResponseEntity<ApiResponse<SystemConfig>> updateConfigValue(
            @PathVariable Long id,
            @RequestBody Map<String, String> valueMap) {
        
        try {
            String newValue = valueMap.get("configValue");
            if (newValue == null) {
                return ResponseEntity.ok(ApiResponse.error(400, "配置值不能为空"));
            }
            
            String username = authService.getCurrentUser().getUsername();
            SystemConfig updatedConfig = configService.updateConfigValue(id, newValue, username);
            return ResponseEntity.ok(ApiResponse.success("更新系统配置值成功", updatedConfig));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "更新系统配置值失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新配置值（通过键）
     */
    @PutMapping("/key/{key}/value")
    @AuditOperation(operation = "更新系统配置值", operationType = "UPDATE", resourceType = "SYSTEM_CONFIG")
    public ResponseEntity<ApiResponse<SystemConfig>> updateConfigValueByKey(
            @PathVariable String key,
            @RequestBody Map<String, String> valueMap) {
        
        try {
            String newValue = valueMap.get("configValue");
            if (newValue == null) {
                return ResponseEntity.ok(ApiResponse.error(400, "配置值不能为空"));
            }
            
            String username = authService.getCurrentUser().getUsername();
            SystemConfig updatedConfig = configService.updateConfigValueByKey(key, newValue, username);
            return ResponseEntity.ok(ApiResponse.success("更新系统配置值成功", updatedConfig));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "更新系统配置值失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除配置
     */
    @DeleteMapping("/{id}")
    @AuditOperation(operation = "删除系统配置", operationType = "DELETE", resourceType = "SYSTEM_CONFIG", resourceIdIndex = 0)
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable Long id) {
        try {
            configService.deleteConfig(id);
            return ResponseEntity.ok(ApiResponse.success("删除系统配置成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "删除系统配置失败: " + e.getMessage()));
        }
    }
    
    /**
     * 批量更新配置
     */
    @PutMapping("/batch")
    @AuditOperation(operation = "批量更新系统配置", operationType = "UPDATE", resourceType = "SYSTEM_CONFIG")
    public ResponseEntity<ApiResponse<List<SystemConfig>>> batchUpdateConfigs(@RequestBody Map<String, String> configMap) {
        try {
            String username = authService.getCurrentUser().getUsername();
            List<SystemConfig> updatedConfigs = configService.batchUpdateConfigs(configMap, username);
            return ResponseEntity.ok(ApiResponse.success("批量更新系统配置成功", updatedConfigs));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "批量更新系统配置失败: " + e.getMessage()));
        }
    }
    
    /**
     * 刷新配置缓存
     */
    @PostMapping("/refresh-cache")
    @AuditOperation(operation = "刷新系统配置缓存", operationType = "REFRESH", resourceType = "SYSTEM_CONFIG_CACHE")
    public ResponseEntity<ApiResponse<Void>> refreshCache() {
        try {
            configService.refreshCache();
            return ResponseEntity.ok(ApiResponse.success("系统配置缓存刷新成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "系统配置缓存刷新失败: " + e.getMessage()));
        }
    }
} 