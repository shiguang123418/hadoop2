package org.shiguang.module.spark.client;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.shiguang.utils.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import scala.Tuple2;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spark客户端工具类
 * 提供Spark操作的便捷方法
 */
@Component
public class SparkClient {
    private static final Logger logger = LoggerFactory.getLogger(SparkClient.class);
    
    @Value("${spark.app.name:HadoopSparkApp}")
    private String appName;
    
    @Value("${spark.master:local[*]}")
    private String master;
    
    @Value("${spark.executor.memory:1g}")
    private String executorMemory;
    
    @Value("${spark.driver.memory:1g}")
    private String driverMemory;
    
    private SparkConf sparkConf;
    private JavaSparkContext sparkContext;
    private SparkSession sparkSession;
    
    /**
     * 初始化Spark连接
     */
    @PostConstruct
    public void init() {
        try {
            sparkConf = new SparkConf()
                    .setAppName(appName)
                    .setMaster(master)
                    .set("spark.executor.memory", executorMemory)
                    .set("spark.driver.memory", driverMemory);
            
            // 创建JavaSparkContext
            sparkContext = new JavaSparkContext(sparkConf);
            
            // 创建SparkSession
            sparkSession = SparkSession.builder()
                    .config(sparkConf)
                    .getOrCreate();
            
            logger.info("Spark客户端初始化完成，连接到: {}", master);
        } catch (Exception e) {
            logger.error("Spark客户端初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("初始化Spark客户端失败", e);
        }
    }
    
    /**
     * 关闭Spark连接
     */
    @PreDestroy
    public void close() {
        if (sparkContext != null) {
            sparkContext.close();
            logger.info("Spark上下文已关闭");
        }
        
        if (sparkSession != null) {
            sparkSession.close();
            logger.info("Spark会话已关闭");
        }
    }
    
    /**
     * 获取JavaSparkContext
     */
    public JavaSparkContext getSparkContext() {
        return sparkContext;
    }
    
    /**
     * 获取SparkSession
     */
    public SparkSession getSparkSession() {
        return sparkSession;
    }
    
    /**
     * 从文本文件创建RDD
     * @param filePath 文件路径（本地或HDFS）
     * @return 文本数据RDD
     */
    public JavaRDD<String> textFile(String filePath) {
        return sparkContext.textFile(filePath);
    }
    
    /**
     * 从CSV文件加载DataFrame
     * @param filePath 文件路径（本地或HDFS）
     * @param header 是否有标题行
     * @param inferSchema 是否推断数据类型
     * @return 数据集
     */
    public Dataset<Row> readCsv(String filePath, boolean header, boolean inferSchema) {
        return sparkSession.read()
                .option("header", header)
                .option("inferSchema", inferSchema)
                .csv(filePath);
    }
    
    /**
     * 从JSON文件加载DataFrame
     * @param filePath 文件路径（本地或HDFS）
     * @return 数据集
     */
    public Dataset<Row> readJson(String filePath) {
        return sparkSession.read().json(filePath);
    }
    
    /**
     * 从Parquet文件加载DataFrame
     * @param filePath 文件路径（本地或HDFS）
     * @return 数据集
     */
    public Dataset<Row> readParquet(String filePath) {
        return sparkSession.read().parquet(filePath);
    }
    
    /**
     * 执行SQL查询
     * @param sql SQL查询语句
     * @return 查询结果
     */
    public Dataset<Row> sql(String sql) {
        return sparkSession.sql(sql);
    }
    
    /**
     * 将DataFrame保存为CSV文件
     * @param dataset 数据集
     * @param outputPath 输出路径
     * @param header 是否输出标题行
     */
    public void saveCsv(Dataset<Row> dataset, String outputPath, boolean header) {
        dataset.write()
                .option("header", header)
                .csv(outputPath);
    }
    
    /**
     * 将DataFrame保存为JSON文件
     * @param dataset 数据集
     * @param outputPath 输出路径
     */
    public void saveJson(Dataset<Row> dataset, String outputPath) {
        dataset.write().json(outputPath);
    }
    
    /**
     * 将DataFrame保存为Parquet文件
     * @param dataset 数据集
     * @param outputPath 输出路径
     */
    public void saveParquet(Dataset<Row> dataset, String outputPath) {
        dataset.write().parquet(outputPath);
    }
    
    /**
     * 执行Spark作业并获取结果
     * @param datasetPath 数据文件路径
     * @param format 文件格式 (csv, json, parquet)
     * @param sql SQL查询
     * @return 查询结果转换为Map列表
     */
    public List<Map<String, Object>> executeJob(String datasetPath, String format, String sql) {
        Dataset<Row> dataset;
        
        // 根据文件格式加载数据集
        switch (format.toLowerCase()) {
            case "csv":
                dataset = readCsv(datasetPath, true, true);
                break;
            case "json":
                dataset = readJson(datasetPath);
                break;
            case "parquet":
                dataset = readParquet(datasetPath);
                break;
            default:
                throw new IllegalArgumentException("不支持的文件格式: " + format);
        }
        
        // 创建临时视图
        dataset.createOrReplaceTempView("dataset");
        
        // 执行SQL查询
        Dataset<Row> result = sql(sql);
        
        // 转换结果为列表
        return result.toJavaRDD().map(row -> {
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < row.size(); i++) {
                map.put(result.columns()[i], row.get(i));
            }
            return map;
        }).collect();
    }
    
    /**
     * 文本文件字数统计
     * @param filePath 文件路径
     * @return 单词统计结果
     */
    public Map<String, Long> wordCount(String filePath) {
        JavaRDD<String> lines = textFile(filePath);
        
        // 分词并计数
        JavaPairRDD<String, Long> counts = lines
                .flatMap(line -> java.util.Arrays.asList(line.split("\\s+")).iterator())
                .filter(word -> !word.isEmpty())
                .mapToPair(word -> new Tuple2<>(word, 1L))
                .reduceByKey((a, b) -> a + b);
        
        return counts.collectAsMap();
    }
    
    /**
     * 获取DataFrame的统计摘要
     * @param dataset 数据集
     * @param columns 需要统计的列名
     * @return 统计摘要
     */
    public Dataset<Row> getStatistics(Dataset<Row> dataset, String... columns) {
        return dataset.describe(columns);
    }
    
    /**
     * 获取DataFrame的统计摘要(无列名参数)
     * @param dataset 数据集
     * @return 统计摘要
     */
    public Dataset<Row> getStatistics(Dataset<Row> dataset) {
        return dataset.describe();
    }
}