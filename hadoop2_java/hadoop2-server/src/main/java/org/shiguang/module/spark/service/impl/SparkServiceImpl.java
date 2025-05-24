package org.shiguang.module.spark.service.impl;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.shiguang.module.spark.client.SparkClient;
import org.shiguang.module.spark.service.SparkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spark服务接口实现类
 */
@Service
public class SparkServiceImpl implements SparkService {
    private static final Logger logger = LoggerFactory.getLogger(SparkServiceImpl.class);

    @Autowired
    private SparkClient sparkClient;

    @Override
    public Dataset<Row> loadDataset(String filePath, String format) {
        logger.info("加载数据集，路径: {}, 格式: {}", filePath, format);
        
        try {
            switch (format.toLowerCase()) {
                case "csv":
                    return sparkClient.readCsv(filePath, true, true);
                case "json":
                    return sparkClient.readJson(filePath);
                case "parquet":
                    return sparkClient.readParquet(filePath);
                default:
                    throw new IllegalArgumentException("不支持的文件格式: " + format);
            }
        } catch (Exception e) {
            logger.error("加载数据集失败: {}", e.getMessage(), e);
            throw new RuntimeException("加载数据集失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> executeSql(String datasetPath, String format, String sql) {
        logger.info("执行SQL查询，数据集: {}, 格式: {}, SQL: {}", datasetPath, format, sql);
        
        try {
            return sparkClient.executeJob(datasetPath, format, sql);
        } catch (Exception e) {
            logger.error("执行SQL查询失败: {}", e.getMessage(), e);
            throw new RuntimeException("执行SQL查询失败", e);
        }
    }

    @Override
    public Map<String, Long> wordCount(String filePath) {
        logger.info("执行单词统计，文件路径: {}", filePath);
        
        try {
            return sparkClient.wordCount(filePath);
        } catch (Exception e) {
            logger.error("执行单词统计失败: {}", e.getMessage(), e);
            throw new RuntimeException("执行单词统计失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> getStatistics(String datasetPath, String format) {
        logger.info("获取数据集统计信息，数据集: {}, 格式: {}", datasetPath, format);
        
        try {
            // 加载数据集
            Dataset<Row> dataset = loadDataset(datasetPath, format);
            
            // 获取统计信息
            Dataset<Row> stats = sparkClient.getStatistics(dataset);
            
            // 转换结果为列表
            return stats.toJavaRDD().map(row -> {
                Map<String, Object> map = new HashMap<>();
                for (int i = 0; i < row.size(); i++) {
                    map.put(stats.columns()[i], row.get(i));
                }
                return map;
            }).collect();
        } catch (Exception e) {
            logger.error("获取数据集统计信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取数据集统计信息失败", e);
        }
    }

    @Override
    public Map<String, Object> saveDataset(String datasetPath, String outputPath, 
                                          String sourceFormat, String targetFormat) {
        logger.info("保存数据集，源路径: {}, 目标路径: {}, 源格式: {}, 目标格式: {}", 
                 datasetPath, outputPath, sourceFormat, targetFormat);
        
        try {
            // 加载源数据集
            Dataset<Row> dataset = loadDataset(datasetPath, sourceFormat);
            
            // 保存为目标格式
            switch (targetFormat.toLowerCase()) {
                case "csv":
                    sparkClient.saveCsv(dataset, outputPath, true);
                    break;
                case "json":
                    sparkClient.saveJson(dataset, outputPath);
                    break;
                case "parquet":
                    sparkClient.saveParquet(dataset, outputPath);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的目标文件格式: " + targetFormat);
            }
            
            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("sourcePath", datasetPath);
            result.put("outputPath", outputPath);
            result.put("sourceFormat", sourceFormat);
            result.put("targetFormat", targetFormat);
            result.put("rowCount", dataset.count());
            
            return result;
        } catch (Exception e) {
            logger.error("保存数据集失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存数据集失败", e);
        }
    }
} 