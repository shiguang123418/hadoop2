package com.hadoop2.server.repository;

import com.hadoop2.server.entity.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Long> {
    // 可以添加自定义查询方法
    boolean existsByName(String name);
} 