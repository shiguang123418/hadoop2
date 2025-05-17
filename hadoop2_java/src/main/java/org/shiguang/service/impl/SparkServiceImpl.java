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

    @Value("${spark.enabled:false}")
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
    
    /**
     * 获取模拟的作物统计数据
     */
    private Map<String, Object> getMockStatistics(String cropName) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("平均产量", 85.6);
        stats.put("产量标准差", 12.8);
        stats.put("最低产量", 62.1);
        stats.put("最高产量", 98.7);
        stats.put("数据量", 24L);
        
        List<Map<String, Object>> trend = new ArrayList<>();
        int baseYear = 2020;
        
        for (int i = 0; i < 5; i++) {
            trend.add(Map.of(
                "年份", baseYear + i,
                "平均产量", 75.0 + i * 3.5
            ));
        }
        
        stats.put("年度趋势", trend);
        stats.put("模拟数据", true);
        return stats;
    }
    
    /**
     * 获取模拟的相关性数据
     */
    private List<Map<String, Object>> getMockCorrelation() {
        List<Map<String, Object>> correlations = new ArrayList<>();
        
        Map<String, Object> temp = new HashMap<>();
        temp.put("factor", "温度");
        temp.put("correlation", 0.82);
        correlations.add(temp);
        
        Map<String, Object> rain = new HashMap<>();
        rain.put("factor", "降雨量");
        rain.put("correlation", 0.45);
        correlations.add(rain);
        
        Map<String, Object> ph = new HashMap<>();
        ph.put("factor", "土壤酸碱度");
        ph.put("correlation", -0.34);
        correlations.add(ph);
        
        return correlations;
    }
    
    /**
     * 获取模拟的预测数据
     */
    private List<Map<String, Object>> getMockForecast(int periods) {
        List<Map<String, Object>> forecast = new ArrayList<>();
        int baseYear = 2025;
        double baseProduction = 90.5;
        
        for (int i = 0; i < periods; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("year", baseYear + i);
            item.put("forecasted_production", baseProduction + i * 2.3);
            forecast.add(item);
        }
        
        return forecast;
    }
    
    /**
     * 获取模拟的最佳生长条件
     */
    private Map<String, Object> getMockOptimalConditions(String cropName) {
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("作物", cropName);
        conditions.put("温度范围", Map.of("最小值", 22.5, "最大值", 28.2));
        conditions.put("降雨量范围", Map.of("最小值", 800.0, "最大值", 1200.0));
        conditions.put("PH值范围", Map.of("最小值", 6.2, "最大值", 7.8));
        conditions.put("预期平均产量", 95.3);
        conditions.put("模拟数据", true);
        return conditions;
    }
    
    /**
     * 获取模拟的聚类数据
     */
    private Map<String, Object> getMockClusters(int numClusters) {
        Map<String, Object> results = new HashMap<>();
        results.put("聚类数", numClusters);
        
        List<Map<String, Object>> clusters = new ArrayList<>();
        String[] cropTypes = {"小麦", "玉米", "水稻", "大豆", "马铃薯"};
        
        for (int i = 0; i < numClusters; i++) {
            Map<String, Object> cluster = new HashMap<>();
            cluster.put("聚类ID", i);
            cluster.put("记录数", 10 + i * 5);
            
            List<String> crops = new ArrayList<>();
            for (int j = 0; j < (i+1) * 2 && j < cropTypes.length; j++) {
                crops.add(cropTypes[j]);
            }
            cluster.put("作物", crops);
            
            cluster.put("平均温度", 20 + i * 2.5);
            cluster.put("平均降雨量", 750 + i * 150.0);
            cluster.put("平均PH值", 5.5 + i * 0.5);
            cluster.put("平均产量", 75.0 + i * 10.0);
            
            clusters.add(cluster);
        }
        
        results.put("聚类结果", clusters);
        results.put("评估指标", Map.of("轮廓系数近似值", 0.65));
        results.put("模拟数据", true);
        
        return results;
    }

    @Override
    public Map<String, Object> calculateCropStatistics(String cropName) {
        logger.info("计算作物'{}'的统计信息", cropName);
        
        // 如果Spark未启用，返回模拟数据
        if (!isSparkAvailable()) {
            logger.info("Spark未启用，返回模拟数据");
            return getMockStatistics(cropName);
        }
        
        try {
            Dataset<Row> df = loadDataFrameFromHDFS();
            Dataset<Row> cropDf = df.filter(df.col("Crop").equalTo(cropName));

            if (cropDf.count() == 0) {
                return Map.of("error", "未找到该作物的数据");
            }

            // 计算基本统计量
            Dataset<Row> stats = cropDf.select(
                    functions.mean("Production").as("平均产量"),
                    functions.stddev("Production").as("产量标准差"),
                    functions.min("Production").as("最低产量"),
                    functions.max("Production").as("最高产量"),
                    functions.count("Production").as("数据量")
            );

            // 转换为Map
            Map<String, Object> results = new HashMap<>();
            Row statsRow = stats.first();
            results.put("平均产量", statsRow.getDouble(0));
            results.put("产量标准差", statsRow.getDouble(1));
            results.put("最低产量", statsRow.getDouble(2));
            results.put("最高产量", statsRow.getDouble(3));
            results.put("数据量", statsRow.getLong(4));

            // 计算按年份的平均产量趋势
            List<Row> yearlyTrend = cropDf.groupBy("Year")
                    .agg(functions.mean("Production").as("平均产量"))
                    .orderBy("Year")
                    .collectAsList();

            List<Map<String, Object>> trend = new ArrayList<>();
            for (Row row : yearlyTrend) {
                trend.add(Map.of(
                        "年份", row.getInt(0),
                        "平均产量", row.getDouble(1)
                ));
            }
            results.put("年度趋势", trend);

            return results;
        } catch (Exception e) {
            logger.error("计算统计信息时发生错误", e);
            return Map.of("error", "计算统计信息失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> analyzeCorrelation(String cropName) {
        logger.info("分析作物'{}'的相关性", cropName);
        
        // 如果Spark未启用，返回模拟数据
        if (!isSparkAvailable()) {
            logger.info("Spark未启用，返回模拟相关性数据");
            return getMockCorrelation();
        }
        
        try {
            Dataset<Row> df = loadDataFrameFromHDFS();
            Dataset<Row> cropDf = df.filter(df.col("Crop").equalTo(cropName));

            if (cropDf.count() == 0) {
                return List.of(Map.of("error", "未找到该作物的数据"));
            }

            // 选择数值列进行相关性分析
            Dataset<Row> features = cropDf.select("Temperature", "Rainfall", "Ph", "Production");
            
            // 计算相关系数
            String[] featureColumns = {"Temperature", "Rainfall", "Ph", "Production"};
            List<Map<String, Object>> correlations = new ArrayList<>();
            
            for (String column : featureColumns) {
                if (!column.equals("Production")) {
                    double correlation = calculatePearsonCorrelation(cropDf, column, "Production");
                    Map<String, Object> result = new HashMap<>();
                    result.put("factor", getChineseName(column));
                    result.put("correlation", correlation);
                    correlations.add(result);
                }
            }
            
            // 按相关性强度排序
            correlations.sort((a, b) -> Double.compare(Math.abs((Double) b.get("correlation")), Math.abs((Double) a.get("correlation"))));
            
            return correlations;
        } catch (Exception e) {
            logger.error("计算相关性时发生错误", e);
            return List.of(Map.of("error", "计算相关性失败: " + e.getMessage()));
        }
    }
    
    private String getChineseName(String column) {
        switch (column) {
            case "Temperature": return "温度";
            case "Rainfall": return "降雨量";
            case "Ph": return "土壤酸碱度";
            case "Production": return "产量";
            default: return column;
        }
    }
    
    private double calculatePearsonCorrelation(Dataset<Row> df, String col1, String col2) {
        // 创建特征向量
        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(new String[]{col1, col2})
                .setOutputCol("features");
        
        Dataset<Row> vectorized = assembler.transform(df).select("features");
        
        // 计算相关性
        Row correlationRow = Correlation.corr(vectorized, "features", "pearson").first();
        double correlation = ((org.apache.spark.ml.linalg.Matrix)correlationRow.getAs("pearson_corr")).toArray()[1];
        return correlation;
    }

    @Override
    public List<Map<String, Object>> forecastProduction(String cropName, int periods) {
        logger.info("预测作物'{}'未来{}期的产量", cropName, periods);
        
        // 如果Spark未启用，返回模拟数据
        if (!isSparkAvailable()) {
            logger.info("Spark未启用，返回模拟预测数据");
            return getMockForecast(periods);
        }
        
        try {
            Dataset<Row> df = loadDataFrameFromHDFS();
            Dataset<Row> cropDf = df.filter(df.col("Crop").equalTo(cropName));

            if (cropDf.count() == 0) {
                return List.of(Map.of("error", "未找到该作物的数据"));
            }

            // 计算历史平均值
            List<Row> yearlyAvg = cropDf.groupBy("Year")
                    .agg(functions.avg("Production").as("avg_production"))
                    .orderBy("Year")
                    .collectAsList();

            // 简单的移动平均预测 (示例性实现，实际项目中可以使用更复杂的时间序列模型)
            List<Map<String, Object>> forecast = new ArrayList<>();
            
            // 使用最后3年的平均值作为趋势
            int dataSize = yearlyAvg.size();
            double avgTrend = 0.0;
            
            if (dataSize >= 3) {
                double lastYearValue = yearlyAvg.get(dataSize - 1).getDouble(1);
                double prevYearValue = yearlyAvg.get(dataSize - 2).getDouble(1);
                double thirdLastYearValue = yearlyAvg.get(dataSize - 3).getDouble(1);
                
                // 计算年增长率
                avgTrend = ((lastYearValue - thirdLastYearValue) / 2) / thirdLastYearValue;
            }
            
            // 获取最后一年的数据
            int lastYear = yearlyAvg.get(dataSize - 1).getInt(0);
            double lastValue = yearlyAvg.get(dataSize - 1).getDouble(1);
            
            // 生成预测
            for (int i = 1; i <= periods; i++) {
                int forecastYear = lastYear + i;
                double forecastValue = lastValue * (1 + avgTrend * i);
                
                forecast.add(Map.of(
                    "year", forecastYear,
                    "forecasted_production", forecastValue
                ));
            }
            
            return forecast;
        } catch (Exception e) {
            logger.error("预测作物产量时发生错误", e);
            return List.of(Map.of("error", "预测产量失败: " + e.getMessage()));
        }
    }

    @Override
    public List<Map<String, Object>> aggregateByGroup(String groupBy) {
        logger.info("按'{}'字段进行分组聚合分析", groupBy);
        
        // 如果Spark未启用，返回空列表
        if (!isSparkAvailable()) {
            logger.info("Spark未启用，返回简单分组数据");
            List<Map<String, Object>> mockData = new ArrayList<>();
            String[] groups = {"小麦", "水稻", "玉米", "大豆"};
            
            for (int i = 0; i < groups.length; i++) {
                Map<String, Object> item = new HashMap<>();
                item.put("分组", groups[i]);
                item.put("平均产量", 75.0 + i * 5);
                item.put("总产量", 750.0 + i * 50);
                item.put("记录数", 10L);
                mockData.add(item);
            }
            
            return mockData;
        }
        
        try {
            Dataset<Row> df = loadDataFrameFromHDFS();
            
            if (!Arrays.asList("Year", "Region", "Crop").contains(groupBy)) {
                return List.of(Map.of("error", "不支持的分组字段: " + groupBy));
            }
            
            // 分组聚合
            Dataset<Row> groupedDf = df.groupBy(groupBy)
                    .agg(
                        functions.avg("Production").as("平均产量"),
                        functions.sum("Production").as("总产量"),
                        functions.count("Production").as("记录数")
                    )
                    .orderBy(functions.desc("平均产量"));
            
            // 转换为结果列表
            List<Row> rows = groupedDf.collectAsList();
            List<Map<String, Object>> results = new ArrayList<>();
            
            for (Row row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("分组", row.get(0).toString());
                item.put("平均产量", row.getDouble(1));
                item.put("总产量", row.getDouble(2));
                item.put("记录数", row.getLong(3));
                results.add(item);
            }
            
            return results;
        } catch (Exception e) {
            logger.error("分组聚合分析失败", e);
            return List.of(Map.of("error", "分组聚合分析失败: " + e.getMessage()));
        }
    }

    @Override
    public Map<String, Object> findOptimalGrowingConditions(String cropName) {
        logger.info("分析作物'{}'的最佳生长条件", cropName);
        
        // 如果Spark未启用，返回模拟数据
        if (!isSparkAvailable()) {
            logger.info("Spark未启用，返回模拟最佳生长条件");
            return getMockOptimalConditions(cropName);
        }
        
        try {
            Dataset<Row> df = loadDataFrameFromHDFS();
            Dataset<Row> cropDf = df.filter(df.col("Crop").equalTo(cropName));

            if (cropDf.count() == 0) {
                return Map.of("error", "未找到该作物的数据");
            }

            // 找出产量前20%的记录
            Dataset<Row> topProduction = cropDf
                    .orderBy(functions.desc("Production"))
                    .limit((int)(cropDf.count() * 0.2));
            
            // 计算最佳条件范围
            Row conditions = topProduction.agg(
                functions.min("Temperature").as("minTemp"),
                functions.max("Temperature").as("maxTemp"),
                functions.min("Rainfall").as("minRain"),
                functions.max("Rainfall").as("maxRain"),
                functions.min("Ph").as("minPh"),
                functions.max("Ph").as("maxPh"),
                functions.avg("Production").as("avgProduction")
            ).first();

            Map<String, Object> results = new HashMap<>();
            results.put("作物", cropName);
            results.put("温度范围", Map.of("最小值", conditions.getDouble(0), "最大值", conditions.getDouble(1)));
            results.put("降雨量范围", Map.of("最小值", conditions.getDouble(2), "最大值", conditions.getDouble(3)));
            results.put("PH值范围", Map.of("最小值", conditions.getDouble(4), "最大值", conditions.getDouble(5)));
            results.put("预期平均产量", conditions.getDouble(6));

            return results;
        } catch (Exception e) {
            logger.error("分析最佳生长条件时发生错误", e);
            return Map.of("error", "分析最佳生长条件失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> clusterCrops(int numClusters) {
        logger.info("执行作物聚类分析，聚类数: {}", numClusters);
        
        // 如果Spark未启用，返回模拟数据
        if (!isSparkAvailable()) {
            logger.info("Spark未启用，返回模拟聚类数据");
            return getMockClusters(numClusters);
        }
        
        try {
            Dataset<Row> df = loadDataFrameFromHDFS();
            
            // 准备特征数据
            VectorAssembler assembler = new VectorAssembler()
                    .setInputCols(new String[]{"Temperature", "Rainfall", "Ph"})
                    .setOutputCol("features");
            
            Dataset<Row> featuresDf = assembler.transform(df);
            
            // 执行KMeans聚类
            KMeans kmeans = new KMeans()
                    .setK(numClusters)
                    .setFeaturesCol("features")
                    .setPredictionCol("cluster");
            
            KMeansModel model = kmeans.fit(featuresDf);
            
            // 获取聚类结果
            Dataset<Row> predictions = model.transform(featuresDf);
            
            // 计算每个聚类中的作物分布
            List<Row> clusterStats = predictions.groupBy("cluster")
                    .agg(
                        functions.count("*").as("count"),
                        functions.collect_list("Crop").as("crops"),
                        functions.avg("Temperature").as("avg_temp"),
                        functions.avg("Rainfall").as("avg_rain"),
                        functions.avg("Ph").as("avg_ph"),
                        functions.avg("Production").as("avg_production")
                    )
                    .orderBy("cluster")
                    .collectAsList();
            
            List<Map<String, Object>> clusters = new ArrayList<>();
            for (Row row : clusterStats) {
                int cluster = row.getInt(0);
                long count = row.getLong(1);
                List<String> crops = new ArrayList<>();
                for (Object crop : row.getList(2)) {
                    if (!crops.contains(crop.toString())) {
                        crops.add(crop.toString());
                    }
                }
                
                Map<String, Object> clusterInfo = new HashMap<>();
                clusterInfo.put("聚类ID", cluster);
                clusterInfo.put("记录数", count);
                clusterInfo.put("作物", crops);
                clusterInfo.put("平均温度", row.getDouble(3));
                clusterInfo.put("平均降雨量", row.getDouble(4));
                clusterInfo.put("平均PH值", row.getDouble(5));
                clusterInfo.put("平均产量", row.getDouble(6));
                
                clusters.add(clusterInfo);
            }
            
            // 计算聚类结果评估指标
            double silhouetteScore = model.summary().trainingCost() / predictions.count();
            
            Map<String, Object> results = new HashMap<>();
            results.put("聚类数", numClusters);
            results.put("聚类结果", clusters);
            results.put("评估指标", Map.of("轮廓系数近似值", silhouetteScore));
            
            return results;
        } catch (Exception e) {
            logger.error("执行聚类分析时发生错误", e);
            return Map.of("error", "执行聚类分析失败: " + e.getMessage());
        }
    }
} 