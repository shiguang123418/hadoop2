package org.shiguang.repository;

import org.shiguang.entity.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Long> {
    // 可以添加自定义查询方法
    boolean existsByName(String name);
} 