package com.hadoop2.server.service;

import com.hadoop2.server.entity.DataSource;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface DataSourceService {
    // 创建数据源
    @Transactional
    DataSource createDataSource(DataSource dataSource);
    
    // 更新数据源
    @Transactional
    DataSource updateDataSource(DataSource dataSource);
    
    // 删除数据源
    @Transactional
    void deleteDataSource(Long id);
    
    // 获取数据源详情
    DataSource getDataSource(Long id);
    
    // 获取所有数据源
    List<DataSource> getAllDataSources();
    
    // 测试数据源连接
    boolean testConnection(DataSource dataSource);
    
    // 更新数据源状态
    @Transactional
    void updateStatus(Long id, String status);
} 