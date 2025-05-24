package org.shiguang.module.hive.service;

import java.util.List;
import java.util.Map;

/**
 * Hive服务接口
 */
public interface HiveService {
    
    /**
     * 获取Hive连接状态
     * @return 连接状态信息
     */
    Map<String, Object> getConnectionStatus();
    
    /**
     * 获取所有数据库列表
     * @return 数据库列表
     */
    List<String> getDatabases();
    
    /**
     * 获取表列表
     * @param database 数据库名称
     * @return 表列表
     */
    List<String> getTables(String database);
    
    /**
     * 获取表结构
     * @param tableName 表名
     * @param database 数据库名称
     * @return 表结构信息
     */
    List<Map<String, Object>> getTableSchema(String tableName, String database);
    
    /**
     * 执行查询
     * @param sql SQL查询语句
     * @return 查询结果
     */
    List<Map<String, Object>> executeQuery(String sql);
    
    /**
     * 执行更新操作（DDL语句）
     * @param sql SQL更新语句
     * @return 更新结果
     */
    Map<String, Object> executeUpdate(String sql);
} 