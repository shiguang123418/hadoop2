package org.shiguang.module.datasource.controller;

import org.shiguang.entity.DataSource;
import org.shiguang.module.datasource.service.DataSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/datasources")
public class DataSourceController {
    
    private final DataSourceService dataSourceService;

    @Autowired
    public DataSourceController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    @PostMapping
    public ResponseEntity<DataSource> createDataSource(@RequestBody DataSource dataSource) {
        return ResponseEntity.ok(dataSourceService.createDataSource(dataSource));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataSource> updateDataSource(@PathVariable Long id, @RequestBody DataSource dataSource) {
        dataSource.setId(id);
        return ResponseEntity.ok(dataSourceService.updateDataSource(dataSource));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDataSource(@PathVariable Long id) {
        dataSourceService.deleteDataSource(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataSource> getDataSource(@PathVariable Long id) {
        return ResponseEntity.ok(dataSourceService.getDataSource(id));
    }

    @GetMapping
    public ResponseEntity<List<DataSource>> getAllDataSources() {
        return ResponseEntity.ok(dataSourceService.getAllDataSources());
    }

    @PostMapping("/{id}/test-connection")
    public ResponseEntity<Boolean> testConnection(@PathVariable Long id) {
        DataSource dataSource = dataSourceService.getDataSource(id);
        return ResponseEntity.ok(dataSourceService.testConnection(dataSource));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        dataSourceService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }
} 