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
import org.apache.hadoop.fs.FileStatus;
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
import java.util.stream.Collectors;
import java.net.InetAddress;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.conf.Configuration;

import lombok.RequiredArgsConstructor;

/**
 * 产品回归分析数据控制器
 * 提供产品回归数据相关的API接口
 */
@RestController
@RequestMapping("/production")
@RequiredArgsConstructor
public class ProductionController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductionController.class);
    private static final String CSV_FILE_PATH = "/user/data/agriculture/product_regressiondb.csv";
    
    private final HdfsService hdfsService;
    private final Configuration hadoopConfig;
    private final FileSystem fileSystem;
    
    private CSVFormat getCsvFormat() {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreHeaderCase(true)
                .build();
    }
    
    /**
     * 获取所有可用的作物类型
     * @return 作物类型列表
     */
    @GetMapping("/crops")
    public ResponseEntity<?> getCrops() {
        try {
            logger.info("获取作物类型列表");
            
            // 尝试使用HDFS前诊断连接状态
            diagnoseHdfsConnection();
            
            String filePath = "/user/data/agriculture/crops.csv";
            List<String> crops = readCropsFromHdfs(filePath);
            
            if (crops.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "No crops data found in HDFS"));
            }
            
            return ResponseEntity.ok(crops);
        } catch (Exception e) {
            logger.error("获取作物类型时出错", e);
            return ResponseEntity.status(500).body(Map.of("error", "获取作物类型失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取特定作物的产量数据
     * @param crop 作物类型
     * @return 包含各环境因素与产量的数据
     */
    @GetMapping("/data")
    public ResponseEntity<?> getProductionData(@RequestParam String crop) {
        logger.info("获取作物'{}'的产量数据", crop);
        
        try {
            List<Double> rainfall = new ArrayList<>();
            List<Double> temperature = new ArrayList<>();
            List<Double> ph = new ArrayList<>();
            List<Double> production = new ArrayList<>();
            
            try (FSDataInputStream inputStream = hdfsService.getFileSystem().open(new Path(CSV_FILE_PATH));
                 Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                 CSVParser csvParser = new CSVParser(reader, getCsvFormat())) {
                
                for (CSVRecord record : csvParser) {
                    if (record.get("Crop").trim().equalsIgnoreCase(crop.trim())) {
                        rainfall.add(Double.parseDouble(record.get("Rainfall")));
                        temperature.add(Double.parseDouble(record.get("Temperature")));
                        ph.add(Double.parseDouble(record.get("Ph")));
                        production.add(Double.parseDouble(record.get("Production")));
                    }
                }
            }
            
            if (rainfall.isEmpty()) {
                logger.info("未找到作物'{}'的数据", crop);
                return ResponseEntity.status(404).body(Map.of("error", "No data found for crop: " + crop));
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("Rainfall", rainfall);
            result.put("Temperature", temperature);
            result.put("Ph", ph);
            result.put("Production", production);
            
            return ResponseEntity.ok(result);
            
        } catch (IOException e) {
            logger.error("读取HDFS CSV文件失败", e);
            return ResponseEntity.status(500).body(Map.of("error", "读取HDFS文件失败: " + e.getMessage()));
        } catch (NumberFormatException e) {
            logger.error("解析数值失败", e);
            return ResponseEntity.status(500).body(Map.of("error", "数据格式错误: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有作物的平均产量数据
     * @return 作物与平均产量的映射
     */
    @GetMapping("/average")
    public ResponseEntity<?> getAverageProduction() {
        logger.info("获取所有作物的平均产量数据");
        
        try {
            Map<String, List<Double>> cropProduction = new HashMap<>();
            
            try (FSDataInputStream inputStream = hdfsService.getFileSystem().open(new Path(CSV_FILE_PATH));
                 Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                 CSVParser csvParser = new CSVParser(reader, getCsvFormat())) {
                
                for (CSVRecord record : csvParser) {
                    String crop = record.get("Crop").trim();
                    double production = Double.parseDouble(record.get("Production"));
                    
                    if (!cropProduction.containsKey(crop)) {
                        cropProduction.put(crop, new ArrayList<>());
                    }
                    cropProduction.get(crop).add(production);
                }
            }
            
            if (cropProduction.isEmpty()) {
                logger.info("未找到产量数据");
                return ResponseEntity.status(404).body(Map.of("error", "No production data found"));
            }
            
            // 计算每种作物的平均产量
            Map<String, Double> averageProduction = new HashMap<>();
            for (Map.Entry<String, List<Double>> entry : cropProduction.entrySet()) {
                double average = entry.getValue().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
                averageProduction.put(entry.getKey(), average);
            }
            
            // 按平均产量排序
            List<Map<String, Object>> result = averageProduction.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("crop", entry.getKey());
                    item.put("averageProduction", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
            
        } catch (IOException e) {
            logger.error("读取HDFS CSV文件失败", e);
            return ResponseEntity.status(500).body(Map.of("error", "读取HDFS文件失败: " + e.getMessage()));
        } catch (NumberFormatException e) {
            logger.error("解析数值失败", e);
            return ResponseEntity.status(500).body(Map.of("error", "数据格式错误: " + e.getMessage()));
        }
    }
    
    /**
     * 获取最佳生长条件
     * @param crop 作物类型
     * @return 最佳生长条件参数
     */
    @GetMapping("/optimal-conditions")
    public ResponseEntity<?> getOptimalConditions(@RequestParam String crop) {
        logger.info("获取作物'{}'的最佳生长条件", crop);
        
        try {
            List<Map<String, Object>> records = new ArrayList<>();
            
            try (FSDataInputStream inputStream = hdfsService.getFileSystem().open(new Path(CSV_FILE_PATH));
                 Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                 CSVParser csvParser = new CSVParser(reader, getCsvFormat())) {
                
                for (CSVRecord record : csvParser) {
                    if (record.get("Crop").trim().equalsIgnoreCase(crop.trim())) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("rainfall", Double.parseDouble(record.get("Rainfall")));
                        item.put("temperature", Double.parseDouble(record.get("Temperature")));
                        item.put("ph", Double.parseDouble(record.get("Ph")));
                        item.put("production", Double.parseDouble(record.get("Production")));
                        records.add(item);
                    }
                }
            }
            
            if (records.isEmpty()) {
                logger.info("未找到作物'{}'的最佳生长条件数据", crop);
                return ResponseEntity.status(404).body(Map.of("error", "No optimal conditions data found for crop: " + crop));
            }
            
            // 按产量排序，找出产量最高的记录
            records.sort((a, b) -> Double.compare((double) b.get("production"), (double) a.get("production")));
            
            Map<String, Object> optimalConditions = records.get(0);
            
            return ResponseEntity.ok(optimalConditions);
            
        } catch (IOException e) {
            logger.error("读取HDFS CSV文件失败", e);
            return ResponseEntity.status(500).body(Map.of("error", "读取HDFS文件失败: " + e.getMessage()));
        } catch (NumberFormatException e) {
            logger.error("解析数值失败", e);
            return ResponseEntity.status(500).body(Map.of("error", "数据格式错误: " + e.getMessage()));
        }
    }
    
    /**
     * 诊断HDFS连接状态
     * 记录当前HDFS配置信息和连接状态，供调试使用
     */
    private void diagnoseHdfsConnection() {
        try {
            logger.info("HDFS 连接诊断开始");
            
            // 1. 检查文件系统状态
            boolean exists = fileSystem.exists(new Path("/"));
            logger.info("HDFS根目录访问状态: {}", exists ? "成功" : "失败");
            
            // 2. 显示配置信息
            logger.info("HDFS URI: {}", fileSystem.getUri());
            logger.info("默认文件系统: {}", hadoopConfig.get("fs.defaultFS"));
            logger.info("使用主机名: {}", hadoopConfig.getBoolean("dfs.client.use.datanode.hostname", true));
            logger.info("客户端超时: {}", hadoopConfig.get("dfs.client.socket-timeout"));
            
            // 3. 显示主机名映射
            String shiguangMapping = hadoopConfig.get("dfs.client.resolved.remote.shiguang");
            if (shiguangMapping != null) {
                logger.info("主机映射 - shiguang: {}", shiguangMapping);
            }
            
            // 4. 尝试连接HDFS服务器
            String hdfsHost = fileSystem.getUri().getHost();
            // int hdfsPort = fileSystem.getUri().getPort();
            
            try {
                InetAddress addr = InetAddress.getByName(hdfsHost);
                boolean reachable = addr.isReachable(5000);
                logger.info("HDFS主机 {} 可达性: {}", hdfsHost, reachable ? "可达" : "不可达");
            } catch (Exception e) {
                logger.warn("检查HDFS主机可达性时出错: {}", e.getMessage());
            }
            
            // 5. 列出根目录内容
            try {
                FileStatus[] fileStatuses = fileSystem.listStatus(new Path("/"));
                logger.info("HDFS根目录文件数: {}", fileStatuses.length);
                for (FileStatus status : fileStatuses) {
                    logger.info("HDFS文件: {}, 大小: {}", status.getPath().getName(), status.getLen());
                }
            } catch (Exception e) {
                logger.warn("列出HDFS根目录内容时出错: {}", e.getMessage());
            }
            
            logger.info("HDFS连接诊断完成");
        } catch (Exception e) {
            logger.error("HDFS连接诊断失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 从HDFS中读取作物列表
     * @param filePath 文件路径
     * @return 作物列表
     */
    private List<String> readCropsFromHdfs(String filePath) {
        List<String> crops = new ArrayList<>();
        try {
            if (!hdfsService.exists(filePath)) {
                logger.warn("HDFS文件不存在: {}", filePath);
                return crops;
            }

            try (FSDataInputStream inputStream = hdfsService.getFileSystem().open(new Path(filePath));
                 Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 BufferedReader bufferedReader = new BufferedReader(reader);
                 CSVParser csvParser = new CSVParser(bufferedReader, getCsvFormat())) {

                for (CSVRecord record : csvParser) {
                    crops.add(record.get("crop_name"));
                }
                logger.info("从HDFS成功读取作物数据: 找到{}个作物", crops.size());
            }
        } catch (IOException e) {
            logger.error("读取HDFS作物数据时出错", e);
        }
        return crops;
    }
}