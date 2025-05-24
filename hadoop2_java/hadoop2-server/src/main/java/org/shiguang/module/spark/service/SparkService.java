package org.shiguang.module.spark.service;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.List;
import java.util.Map;

/**
 * Spark服务接口
 * 提供Spark相关操作
 */
public interface SparkService {

    /**
     * 从文件加载数据集
     * @param filePath 文件路径
     * @param format 文件格式 (csv, json, parquet)
     * @return 加载的数据集
     */
    Dataset<Row> loadDataset(String filePath, String format);

    /**
     * 执行SQL查询
     * @param datasetPath 数据集路径
     * @param format 文件格式
     * @param sql SQL查询
     * @return 查询结果
     */
    List<Map<String, Object>> executeSql(String datasetPath, String format, String sql);
    
    /**
     * 执行单词统计
     * @param filePath 文本文件路径
     * @return 单词统计结果
     */
    Map<String, Long> wordCount(String filePath);
    
    /**
     * 获取数据集统计信息
     * @param datasetPath 数据集路径
     * @param format 文件格式
     * @return 统计信息
     */
    List<Map<String, Object>> getStatistics(String datasetPath, String format);
    
    /**
     * 保存数据集到文件系统
     * @param datasetPath 源数据集路径
     * @param outputPath 输出路径
     * @param sourceFormat 源文件格式
     * @param targetFormat 目标文件格式
     * @return 保存结果
     */
    Map<String, Object> saveDataset(String datasetPath, String outputPath, 
                                   String sourceFormat, String targetFormat);
} 