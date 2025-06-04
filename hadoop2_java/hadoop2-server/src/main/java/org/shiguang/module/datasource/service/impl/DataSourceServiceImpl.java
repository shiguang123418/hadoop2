package org.shiguang.module.datasource.service.impl;

import org.shiguang.entity.DataSource;
import org.shiguang.repository.DataSourceRepository;
import org.shiguang.module.datasource.service.DataSourceService;
import org.shiguang.module.datasource.exception.DataSourceException;
import org.shiguang.module.datasource.exception.DataSourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DataSourceServiceImpl implements DataSourceService {
    
    private final DataSourceRepository dataSourceRepository;

    @Autowired
    public DataSourceServiceImpl(DataSourceRepository dataSourceRepository) {
        this.dataSourceRepository = dataSourceRepository;
    }

    @Override
    @Transactional
    public DataSource createDataSource(DataSource dataSource) {
        if (dataSourceRepository.existsByName(dataSource.getName())) {
            throw new DataSourceException("数据源名称已存在");
        }
        
        dataSource.setCreateTime(LocalDateTime.now());
        dataSource.setUpdateTime(LocalDateTime.now());
        dataSource.setStatus("ACTIVE");
        return dataSourceRepository.save(dataSource);
    }

    @Override
    @Transactional
    public DataSource updateDataSource(DataSource dataSource) {
        DataSource existingDataSource = dataSourceRepository.findById(dataSource.getId())
            .orElseThrow(() -> new DataSourceNotFoundException("数据源不存在"));
            
        // 检查名称是否重复（排除自身）
        if (!existingDataSource.getName().equals(dataSource.getName()) 
            && dataSourceRepository.existsByName(dataSource.getName())) {
            throw new DataSourceException("数据源名称已存在");
        }
        
        dataSource.setUpdateTime(LocalDateTime.now());
        return dataSourceRepository.save(dataSource);
    }

    @Override
    @Transactional
    public void deleteDataSource(Long id) {
        if (!dataSourceRepository.existsById(id)) {
            throw new DataSourceNotFoundException("数据源不存在");
        }
        dataSourceRepository.deleteById(id);
    }

    @Override
    public DataSource getDataSource(Long id) {
        return dataSourceRepository.findById(id)
            .orElseThrow(() -> new DataSourceNotFoundException("数据源不存在"));
    }

    @Override
    public List<DataSource> getAllDataSources() {
        return dataSourceRepository.findAll();
    }

    @Override
    public boolean testConnection(DataSource dataSource) {
        // TODO: 实现不同类型数据源的连接测试
        // 这里简单返回true，实际项目中需要根据数据源类型进行真实的连接测试
        return true;
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        DataSource dataSource = getDataSource(id);
        dataSource.setStatus(status);
        dataSource.setUpdateTime(LocalDateTime.now());
        dataSourceRepository.save(dataSource);
    }
} 