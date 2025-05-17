package org.shiguang.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.shiguang.service.HdfsService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import lombok.RequiredArgsConstructor;

/**
 * 气象数据控制器
 * 提供气象数据相关的API接口
 */
@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {
    
    private static final Logger logger = LoggerFactory.getLogger(WeatherController.class);
    private static final String CSV_FILE_PATH = "/user/data/agriculture/temprainfall.csv";
    
    private final HdfsService hdfsService;
    
    /**
     * 获取气象数据
     * @param city 城市名称
     * @param metric 指标类型（maxtemp/mintemp/rainfall）
     * @return 月份和对应的指标值
     */
    @GetMapping("")
    public ResponseEntity<?> getWeatherData(
            @RequestParam String city,
            @RequestParam String metric) {
        
        logger.info("获取气象数据, 城市: {}, 指标: {}", city, metric);
        
        try {
            List<String> months = new ArrayList<>();
            List<Double> values = new ArrayList<>();
            
            // 从HDFS读取CSV文件数据
            try (FSDataInputStream inputStream = hdfsService.getFileSystem().open(new Path(CSV_FILE_PATH));
                 Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
                
                for (CSVRecord record : csvParser) {
                    if (record.get("city").trim().equalsIgnoreCase(city.trim())) {
                        months.add(record.get("month").trim());
                        // 确保CSV文件中有对应的列
                        if (record.isMapped(metric)) {
                            values.add(Double.parseDouble(record.get(metric)));
                        } else {
                            logger.error("CSV文件中没有{}列", metric);
                            return ResponseEntity.badRequest().body("指标数据不存在");
                        }
                    }
                }
            }
            
            if (months.isEmpty()) {
                logger.warn("没有找到城市'{}'的数据", city);
                return ResponseEntity.badRequest().body("没有找到该城市的数据");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("months", months);
            result.put("values", values);
            
            return ResponseEntity.ok(result);
            
        } catch (IOException e) {
            logger.error("读取HDFS CSV文件失败", e);
            return ResponseEntity.internalServerError().body("读取数据失败: " + e.getMessage());
        } catch (NumberFormatException e) {
            logger.error("解析数值失败", e);
            return ResponseEntity.internalServerError().body("数据格式错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取城市的所有气象指标数据
     * @param city 城市名称
     * @return 包含所有指标数据的结果
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllWeatherData(@RequestParam String city) {
        logger.info("获取城市'{}'的所有气象指标数据", city);
        
        try {
            List<String> months = new ArrayList<>();
            List<String> states = new ArrayList<>();
            Map<String, List<Double>> metrics = new LinkedHashMap<>();
            metrics.put("maxtemp", new ArrayList<>());
            metrics.put("mintemp", new ArrayList<>());
            metrics.put("rainfall", new ArrayList<>());
            
            // 从HDFS读取CSV文件数据
            try (FSDataInputStream inputStream = hdfsService.getFileSystem().open(new Path(CSV_FILE_PATH));
                 Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
                
                for (CSVRecord record : csvParser) {
                    if (record.get("city").trim().equalsIgnoreCase(city.trim())) {
                        String month = record.get("month").trim();
                        months.add(month);
                        
                        // 添加state信息
                        if (record.isMapped("state")) {
                            states.add(record.get("state").trim());
                        }
                        
                        // 提取每个指标的数据
                        for (String metric : metrics.keySet()) {
                            if (record.isMapped(metric)) {
                                metrics.get(metric).add(Double.parseDouble(record.get(metric)));
                            } else {
                                logger.error("CSV文件中没有{}列", metric);
                                return ResponseEntity.badRequest().body("指标数据不存在: " + metric);
                            }
                        }
                    }
                }
                
                if (months.isEmpty()) {
                    logger.warn("没有找到城市'{}'的数据", city);
                    return ResponseEntity.badRequest().body("没有找到该城市的数据");
                }
            }
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("months", months);
            if (!states.isEmpty()) {
                result.put("state", states.get(0)); // 假设同一城市的state相同
            }
            result.put("metrics", metrics);
            
            return ResponseEntity.ok(result);
            
        } catch (IOException e) {
            logger.error("读取HDFS CSV文件失败", e);
            return ResponseEntity.internalServerError().body("读取数据失败: " + e.getMessage());
        } catch (NumberFormatException e) {
            logger.error("解析数值失败", e);
            return ResponseEntity.internalServerError().body("数据格式错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取可用城市列表
     * @return 城市列表
     */
    @GetMapping("/cities")
    public ResponseEntity<?> getCities() {
        logger.info("获取可用城市列表");
        
        try {
            Map<String, String> cityStateMap = new HashMap<>();
            
            // 从HDFS读取CSV文件数据
            try (FSDataInputStream inputStream = hdfsService.getFileSystem().open(new Path(CSV_FILE_PATH));
                 Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
                
                for (CSVRecord record : csvParser) {
                    String city = record.get("city").trim();
                    if (record.isMapped("state")) {
                        cityStateMap.put(city, record.get("state").trim());
                    } else {
                        cityStateMap.put(city, "");
                    }
                }
            }
            
            // 构建包含城市和州信息的结果
            List<Map<String, String>> result = new ArrayList<>();
            for (Map.Entry<String, String> entry : cityStateMap.entrySet()) {
                Map<String, String> cityInfo = new HashMap<>();
                cityInfo.put("city", entry.getKey());
                cityInfo.put("state", entry.getValue());
                result.add(cityInfo);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (IOException e) {
            logger.error("读取HDFS CSV文件失败", e);
            return ResponseEntity.internalServerError().body("读取数据失败: " + e.getMessage());
        }
    }
} 