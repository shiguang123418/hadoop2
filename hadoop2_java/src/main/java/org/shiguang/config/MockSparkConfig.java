package org.shiguang.config;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 提供Spark模拟实现的配置类
 * 当出现Spark兼容性问题时使用此配置类提供模拟实现
 */
@Configuration
@ConditionalOnProperty(name = "spark.use-mock", havingValue = "true")
public class MockSparkConfig {

    // Set logger to private to prevent accidental usage elsewhere
    private static final Logger logger = LoggerFactory.getLogger(MockSparkConfig.class);

    @Value("${spark.app.name:农业数据分析Mock-Spark应用}")
    private String appName;

    @Value("${spark.master:local[2]}")
    private String masterUri;

    /**
     * 创建一个Mock的SparkConf
     */
    @Bean
    @Primary
    public SparkConf sparkConf() {
        // Removed logging statement
        SparkConf conf = new SparkConf();
        conf.setAppName(appName);
        conf.setMaster(masterUri);
        return conf;
    }

    /**
     * 创建一个空的JavaSparkContext，实际不会初始化Spark
     */
    @Bean
    @Primary
    public JavaSparkContext javaSparkContext() {
        // Removed logging statement
        return null;  // 返回null，依赖此bean的组件需要进行空检查
    }

    /**
     * 创建一个空的SparkSession，实际不会初始化Spark
     */
    @Bean
    @Primary
    public SparkSession sparkSession() {
        // Removed logging statement
        return null;  // 返回null，依赖此bean的组件需要进行空检查
    }
} 