package org.shiguang.module.spark.controller;

import org.shiguang.module.common.controller.BaseController;
import org.shiguang.module.common.response.ApiResponse;
import org.shiguang.module.spark.service.SparkService;
import org.shiguang.module.spark.client.SparkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spark控制器
 * 提供Spark相关REST接口
 */
@RestController
@RequestMapping("/spark")
public class SparkController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(SparkController.class);

    @Autowired
    private SparkService sparkService;
    
    @Autowired
    private SparkClient sparkClient;
    
    @Value("${spark.master}")
    private String sparkMaster;
    
    @Value("${spark.app.name}")
    private String sparkAppName;

    /**
     * 执行SQL查询
     * @param datasetPath 数据集路径
     * @param format 文件格式
     * @param sql SQL查询语句
     * @return 查询结果
     */
    @PostMapping("/sql")
    public ApiResponse<List<Map<String, Object>>> executeSql(
            @RequestParam("datasetPath") String datasetPath,
            @RequestParam("format") String format,
            @RequestParam("sql") String sql) {
        
        logger.info("接收到SQL查询请求，数据集: {}, 格式: {}, SQL: {}", datasetPath, format, sql);
        
        try {
            List<Map<String, Object>> result = sparkService.executeSql(datasetPath, format, sql);
            return success(result);
        } catch (Exception e) {
            logger.error("执行SQL查询失败: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "执行SQL查询失败: " + e.getMessage(), null);
        }
    }

    /**
     * 执行单词统计
     * @param filePath 文本文件路径
     * @return 单词统计结果
     */
    @GetMapping("/wordcount")
    public ApiResponse<Map<String, Long>> wordCount(@RequestParam("filePath") String filePath) {
        logger.info("接收到单词统计请求，文件路径: {}", filePath);
        
        try {
            Map<String, Long> result = sparkService.wordCount(filePath);
            return success(result);
        } catch (Exception e) {
            logger.error("执行单词统计失败: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "执行单词统计失败: " + e.getMessage(), null);
        }
    }

    /**
     * 获取数据集统计信息
     * @param datasetPath 数据集路径
     * @param format 文件格式
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public ApiResponse<List<Map<String, Object>>> getStatistics(
            @RequestParam("datasetPath") String datasetPath,
            @RequestParam("format") String format) {
        
        logger.info("接收到获取数据集统计信息请求，数据集: {}, 格式: {}", datasetPath, format);
        
        try {
            List<Map<String, Object>> result = sparkService.getStatistics(datasetPath, format);
            return success(result);
        } catch (Exception e) {
            logger.error("获取数据集统计信息失败: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "获取数据集统计信息失败: " + e.getMessage(), null);
        }
    }

    /**
     * 数据集格式转换
     * @param datasetPath 源数据集路径
     * @param outputPath 输出路径
     * @param sourceFormat 源文件格式
     * @param targetFormat 目标文件格式
     * @return 转换结果
     */
    @PostMapping("/convert")
    public ApiResponse<Map<String, Object>> convertDataset(
            @RequestParam("datasetPath") String datasetPath,
            @RequestParam("outputPath") String outputPath,
            @RequestParam("sourceFormat") String sourceFormat,
            @RequestParam("targetFormat") String targetFormat) {
        
        logger.info("接收到数据集格式转换请求，源路径: {}, 目标路径: {}, 源格式: {}, 目标格式: {}", 
                 datasetPath, outputPath, sourceFormat, targetFormat);
        
        try {
            Map<String, Object> result = sparkService.saveDataset(
                    datasetPath, outputPath, sourceFormat, targetFormat);
            return success(result);
        } catch (Exception e) {
            logger.error("数据集格式转换失败: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "数据集格式转换失败: " + e.getMessage(), null);
        }
    }

    /**
     * 获取Spark会话信息
     * @return Spark会话信息
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getSparkInfo() {
        logger.info("接收到获取Spark会话信息请求");
        
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("status", "活跃");
            info.put("master", sparkMaster);
            info.put("appName", sparkAppName);
            
            // 尝试获取Spark会话实例信息
            if (sparkClient != null && sparkClient.getSparkSession() != null) {
                info.put("sparkSessionActive", true);
                info.put("sparkContextActive", sparkClient.getSparkContext() != null);
                info.put("version", sparkClient.getSparkSession().version());
            } else {
                info.put("sparkSessionActive", false);
                info.put("sparkContextActive", false);
                info.put("version", "未知");
            }
            
            return success(info);
        } catch (Exception e) {
            logger.error("获取Spark会话信息失败: {}", e.getMessage(), e);
            return new ApiResponse<>(500, "获取Spark会话信息失败: " + e.getMessage(), null);
        }
    }
}