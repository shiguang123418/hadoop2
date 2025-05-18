package org.shiguang.service.impl;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.clustering.KMeans;
import org.apache.spark.ml.clustering.KMeansModel;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.ml.stat.Correlation;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;
import org.shiguang.service.HdfsService;
import org.shiguang.service.SparkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SparkServiceImpl implements SparkService {

    private static final Logger logger = LoggerFactory.getLogger(SparkServiceImpl.class);
    private static final String CSV_FILE_PATH = "/user/data/agriculture/product_regressiondb.csv";

    @Value("${spark.enabled:true}")
    private boolean sparkEnabled;

    @Autowired(required = false)
    private SparkSession sparkSession;

    @Autowired(required = false)
    private JavaSparkContext javaSparkContext;

    @Autowired
    private HdfsService hdfsService;

    /**
     * 检查Spark是否可用
     */
    private boolean isSparkAvailable() {
        try {
            return sparkEnabled && sparkSession != null && !sparkSession.sparkContext().isStopped();
        } catch (Exception e) {
            // 如果访问sparkContext时发生异常，说明SparkSession不可用
            logger.warn("检查SparkSession可用性时出错", e);
            return false;
        }
    }

    private CSVFormat getCsvFormat() {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreHeaderCase(true)
                .build();
    }

    private Dataset<Row> loadDataFrameFromHDFS() {
        if (!isSparkAvailable()) {
            logger.warn("Spark未启用，无法加载DataFrame");
            throw new RuntimeException("Spark未启用");
        }

        try {
            // 创建Schema
            StructType schema = DataTypes.createStructType(new StructField[] {
                DataTypes.createStructField("Crop", DataTypes.StringType, false),
                DataTypes.createStructField("Year", DataTypes.IntegerType, false),
                DataTypes.createStructField("Region", DataTypes.StringType, false),
                DataTypes.createStructField("Temperature", DataTypes.DoubleType, false),
                DataTypes.createStructField("Rainfall", DataTypes.DoubleType, false),
                DataTypes.createStructField("Ph", DataTypes.DoubleType, false),
                DataTypes.createStructField("Production", DataTypes.DoubleType, false)
            });

            // 从HDFS读取CSV数据
            String hdfsPath = "hdfs://localhost:9000" + CSV_FILE_PATH;
            Dataset<Row> df = sparkSession.read()
                    .option("header", "true")
                    .option("inferSchema", "false")
                    .schema(schema)
                    .csv(hdfsPath);

            return df;
        } catch (Exception e) {
            logger.error("加载DataFrame失败", e);
            throw new RuntimeException("加载数据失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> calculateCropStatistics(String cropName) {
        logger.info("计算{}的统计数据", cropName);
        
        if (!isSparkAvailable()) {
            throw new RuntimeException("Spark服务不可用");
        }
        
        try {
            Dataset<Row> df = loadDataFrameFromHDFS();
            
            // 过滤特定作物的数据
            Dataset<Row> cropData = df.filter(df.col("Crop").equalTo(cropName));
            
            if (cropData.count() == 0) {
                throw new RuntimeException("未找到作物数据: " + cropName);
            }
            
            // 计算基本统计数据
            Dataset<Row> stats = cropData.select(
                functions.avg("Production").alias("平均产量"),
                functions.stddev("Production").alias("产量标准差"),
                functions.min("Production").alias("最低产量"),
                functions.max("Production").alias("最高产量"),
                functions.count("Production").alias("数据量")
            );
            
            Map<String, Object> results = new HashMap<>();
            Row statsRow = stats.first();
            results.put("平均产量", statsRow.getDouble(0));
            results.put("产量标准差", statsRow.getDouble(1));
            results.put("最低产量", statsRow.getDouble(2));
            results.put("最高产量", statsRow.getDouble(3));
            results.put("数据量", statsRow.getLong(4));
            
            // 计算年度趋势
            Dataset<Row> yearlyTrend = cropData.groupBy("Year")
                .agg(functions.avg("Production").alias("平均产量"))
                .orderBy("Year");
                
            List<Map<String, Object>> trend = new ArrayList<>();
            for (Row row : yearlyTrend.collectAsList()) {
                trend.add(Map.of(
                    "年份", row.getInt(0),
                    "平均产量", row.getDouble(1)
                ));
            }
            
            results.put("年度趋势", trend);
            
            return results;
        } catch (Exception e) {
            logger.error("计算作物统计数据时出错", e);
            throw new RuntimeException("计算统计数据失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<Map<String, Object>> analyzeCorrelation(String cropName) {
        logger.info("分析{}的相关性数据", cropName);
        
        if (!isSparkAvailable()) {
            throw new RuntimeException("Spark服务不可用");
        }
        
        try {
            Dataset<Row> df = loadDataFrameFromHDFS();
            
            // 过滤特定作物的数据
            Dataset<Row> cropData = df.filter(df.col("Crop").equalTo(cropName));
            
            if (cropData.count() == 0) {
                throw new RuntimeException("未找到作物数据: " + cropName);
            }
            
            // 计算各因素与产量的相关性
            List<Map<String, Object>> correlations = new ArrayList<>();
            
            String[] factors = {"Temperature", "Rainfall", "Ph"};
            
            for (String factor : factors) {
                double correlation = calculatePearsonCorrelation(cropData, factor, "Production");
                
                Map<String, Object> result = new HashMap<>();
                result.put("factor", getChineseName(factor));
                result.put("correlation", correlation);
                correlations.add(result);
            }
            
            // 按相关系数的绝对值降序排列
            correlations.sort((a, b) -> Double.compare(
                Math.abs((Double) b.get("correlation")),
                Math.abs((Double) a.get("correlation"))
            ));
            
            return correlations;
        } catch (Exception e) {
            logger.error("分析相关性时出错", e);
            throw new RuntimeException("分析相关性失败: " + e.getMessage());
        }
    }
    
    private String getChineseName(String column) {
        switch (column) {
            case "Temperature": return "温度";
            case "Rainfall": return "降雨量";
            case "Ph": return "土壤酸碱度";
            default: return column;
        }
    }
    
    private double calculatePearsonCorrelation(Dataset<Row> df, String col1, String col2) {
        VectorAssembler assembler = new VectorAssembler()
            .setInputCols(new String[] {col1, col2})
            .setOutputCol("features");
        
        Dataset<Row> features = assembler.transform(df);
        Row correlationRow = Correlation.corr(features, "features").first();
        
        return ((org.apache.spark.ml.linalg.Matrix)correlationRow.getAs("pearson(features)")).toArray()[1];
    }
    
    @Override
    public List<Map<String, Object>> forecastProduction(String cropName, int periods) {
        logger.info("预测{}未来{}年的产量", cropName, periods);
        
        if (!isSparkAvailable()) {
            throw new RuntimeException("Spark服务不可用");
        }
        
        try {
            Dataset<Row> df = loadDataFrameFromHDFS();
            
            // 过滤特定作物的数据
            Dataset<Row> cropData = df.filter(df.col("Crop").equalTo(cropName));
            
            if (cropData.count() == 0) {
                throw new RuntimeException("未找到作物数据: " + cropName);
            }
            
            // 按年份分组并计算平均产量
            Dataset<Row> yearlyData = cropData.groupBy("Year")
                .agg(functions.avg("Production").alias("avg_production"))
                .orderBy("Year");
            
            List<Row> rows = yearlyData.collectAsList();
            
            // 获取最后一年
            int lastYear = rows.get(rows.size() - 1).getInt(0);
            
            // 简单线性回归预测
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            int n = rows.size();
            
            for (Row row : rows) {
                int year = row.getInt(0);
                double production = row.getDouble(1);
                
                sumX += year;
                sumY += production;
                sumXY += year * production;
                sumX2 += year * year;
            }
            
            double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
            double intercept = (sumY - slope * sumX) / n;
            
            // 生成预测结果
            List<Map<String, Object>> forecast = new ArrayList<>();
            for (int i = 1; i <= periods; i++) {
                int year = lastYear + i;
                double production = slope * year + intercept;
                
                Map<String, Object> prediction = new HashMap<>();
                prediction.put("year", year);
                prediction.put("forecasted_production", production);
                forecast.add(prediction);
            }
            
            return forecast;
        } catch (Exception e) {
            logger.error("预测产量时出错", e);
            throw new RuntimeException("预测产量失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<Map<String, Object>> aggregateByGroup(String groupBy) {
        logger.info("按{}分组聚合数据", groupBy);
        
        if (!isSparkAvailable()) {
            throw new RuntimeException("Spark服务不可用");
        }
        
        try {
            Dataset<Row> df = loadDataFrameFromHDFS();
            
            // 按指定字段分组
            RelationalGroupedDataset grouped = df.groupBy(groupBy);
            
            // 计算聚合值
            Dataset<Row> aggregated = grouped.agg(
                functions.avg("Production").alias("avg_production"),
                functions.count("*").alias("count")
            ).orderBy(functions.desc("avg_production"));
            
            // 转换为列表
            List<Map<String, Object>> result = new ArrayList<>();
            for (Row row : aggregated.collectAsList()) {
                Map<String, Object> item = new HashMap<>();
                item.put("group", row.get(0).toString());
                item.put("averageProduction", row.getDouble(1));
                item.put("count", row.getLong(2));
                result.add(item);
            }
            
            return result;
        } catch (Exception e) {
            logger.error("聚合数据时出错", e);
            throw new RuntimeException("聚合数据失败: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> findOptimalGrowingConditions(String cropName) {
        logger.info("查找{}的最佳生长条件", cropName);
        
        if (!isSparkAvailable()) {
            throw new RuntimeException("Spark服务不可用");
        }
        
        try {
            Dataset<Row> df = loadDataFrameFromHDFS();
            
            // 过滤特定作物的数据
            Dataset<Row> cropData = df.filter(df.col("Crop").equalTo(cropName));
            
            if (cropData.count() == 0) {
                throw new RuntimeException("未找到作物数据: " + cropName);
            }
            
            // 找出产量最高的前20%记录
            long totalCount = cropData.count();
            long topCount = Math.max(1, totalCount / 5);
            
            Dataset<Row> topProductions = cropData.orderBy(functions.desc("Production")).limit((int)topCount);
            
            // 计算最佳条件范围
            Row stats = topProductions.select(
                functions.min("Temperature").alias("min_temp"),
                functions.max("Temperature").alias("max_temp"),
                functions.min("Rainfall").alias("min_rainfall"),
                functions.max("Rainfall").alias("max_rainfall"),
                functions.min("Ph").alias("min_ph"),
                functions.max("Ph").alias("max_ph"),
                functions.avg("Production").alias("avg_production")
            ).first();
            
            // 构建结果
            Map<String, Object> conditions = new HashMap<>();
            conditions.put("作物", cropName);
            conditions.put("温度范围", Map.of("最小值", stats.getDouble(0), "最大值", stats.getDouble(1)));
            conditions.put("降雨量范围", Map.of("最小值", stats.getDouble(2), "最大值", stats.getDouble(3)));
            conditions.put("PH值范围", Map.of("最小值", stats.getDouble(4), "最大值", stats.getDouble(5)));
            conditions.put("预期平均产量", stats.getDouble(6));
            
            return conditions;
        } catch (Exception e) {
            logger.error("查找最佳生长条件时出错", e);
            throw new RuntimeException("查找最佳生长条件失败: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> clusterCrops(int numClusters) {
        logger.info("将作物数据分为{}个聚类", numClusters);
        
        if (!isSparkAvailable()) {
            throw new RuntimeException("Spark服务不可用");
        }
        
        try {
            Dataset<Row> df = loadDataFrameFromHDFS();
            
            // 准备聚类特征列
            String[] featureCols = {"Temperature", "Rainfall", "Ph", "Production"};
            
            VectorAssembler assembler = new VectorAssembler()
                .setInputCols(featureCols)
                .setOutputCol("features");
            
            Dataset<Row> featureData = assembler.transform(df);
            
            // 应用KMeans聚类
            KMeans kmeans = new KMeans()
                .setK(numClusters)
                .setSeed(42)
                .setFeaturesCol("features")
                .setPredictionCol("cluster");
            
            KMeansModel model = kmeans.fit(featureData);
            
            // 添加聚类标签
            Dataset<Row> clustered = model.transform(featureData);
            
            // 计算聚类中心
            Vector[] clusterCenters = model.clusterCenters();
            
            // 计算每个聚类的统计信息
            List<Map<String, Object>> clusters = new ArrayList<>();
            for (int i = 0; i < numClusters; i++) {
                Dataset<Row> clusterData = clustered.filter(clustered.col("cluster").equalTo(i));
                
                if (clusterData.count() == 0) {
                    continue;
                }
                
                // 获取该聚类中的作物
                List<String> crops = clusterData.select("Crop").distinct()
                    .collectAsList()
                    .stream()
                    .map(row -> row.getString(0))
                    .collect(Collectors.toList());
                
                // 计算聚类统计信息
                Row clusterStats = clusterData.select(
                    functions.avg("Temperature").alias("avg_temp"),
                    functions.avg("Rainfall").alias("avg_rainfall"),
                    functions.avg("Ph").alias("avg_ph"),
                    functions.avg("Production").alias("avg_production"),
                    functions.count("*").alias("count")
                ).first();
                
                // 构建聚类信息
                Map<String, Object> cluster = new HashMap<>();
                cluster.put("聚类ID", i);
                cluster.put("作物", crops);
                cluster.put("样本数", clusterStats.getLong(4));
                cluster.put("平均温度", clusterStats.getDouble(0));
                cluster.put("平均降雨量", clusterStats.getDouble(1));
                cluster.put("平均PH值", clusterStats.getDouble(2));
                cluster.put("平均产量", clusterStats.getDouble(3));
                
                clusters.add(cluster);
            }
            
            // 返回结果
            Map<String, Object> results = new HashMap<>();
            results.put("聚类数", numClusters);
            results.put("聚类", clusters);
            
            return results;
        } catch (Exception e) {
            logger.error("聚类分析时出错", e);
            throw new RuntimeException("聚类分析失败: " + e.getMessage());
        }
    }
} 