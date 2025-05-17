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
import org.springframework.context.annotation.PropertySource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import javax.annotation.PostConstruct;

@Configuration
@Conditional(SparkEnabledCondition.class)
@ConditionalOnProperty(name = "spark.use-mock", havingValue = "false", matchIfMissing = true)
public class SparkConfig {

    // 静态初始化块，在类加载时设置系统属性，确保在Spark类被加载前设置
    static {
        // 设置Spark日志系统为SLF4J
        System.setProperty("org.apache.spark.internal.Logging.outputLogger", "SLF4J");
        
        // 禁用JMX以避免日志冲突
        System.setProperty("log4j2.disable.jmx", "true");
        
        // 配置log4j2
        System.setProperty("log4j.configurationFile", "log4j2.xml");
        
        // 确保将Log4j日志级别设为ERROR
        System.setProperty("org.apache.logging.log4j.level", "ERROR");
    }

    @PostConstruct
    public void init() {
        // 在Bean初始化时设置属性
        logger.info("初始化SparkConfig，设置日志系统...");
        
        // 这样确保在任何Spark相关类初始化前设置系统属性
        System.setProperty("spark.driver.extraJavaOptions", 
                "-Dlog4j2.disable.jmx=true -Dorg.apache.spark.internal.Logging.outputLogger=SLF4J");
        System.setProperty("spark.executor.extraJavaOptions", 
                "-Dlog4j2.disable.jmx=true -Dorg.apache.spark.internal.Logging.outputLogger=SLF4J");
    }

    private static final Logger logger = LoggerFactory.getLogger(SparkConfig.class);

    @Value("${spark.app.name:农业数据分析Spark应用}")
    private String appName;

    @Value("${spark.master:local[2]}")
    private String masterUri;
    
    @Value("${spark.enabled:false}")
    private boolean sparkEnabled;

    @Bean
    @Conditional(SparkEnabledCondition.class)
    public SparkConf sparkConf() {
        if (!sparkEnabled) {
            logger.info("Spark未启用，返回null SparkConf");
            return null;
        }
        
        logger.info("初始化Spark配置: app={}, master={}", appName, masterUri);
        
        // 这里不再需要设置日志系统属性，已在静态初始化块中完成
        
        return new SparkConf()
                .setAppName(appName)
                .setMaster(masterUri)
                .set("spark.driver.allowMultipleContexts", "true")
                .set("spark.ui.enabled", "false")
                .set("spark.driver.userClassPathFirst", "true") 
                .set("spark.executor.userClassPathFirst", "true")
                .set("spark.hadoop.fs.permissions.umask-mode", "022");
    }

    @Bean
    @Conditional(SparkEnabledCondition.class)
    public JavaSparkContext javaSparkContext(SparkConf sparkConf) {
        if (sparkConf == null || !sparkEnabled) {
            logger.info("Spark未启用或SparkConf为null，返回null JavaSparkContext");
            return null;
        }
        
        logger.info("初始化JavaSparkContext");
        try {
            // 确保使用适当的类加载器
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            
            // 在创建JavaSparkContext前再次确保日志系统已配置
            System.setProperty("org.apache.spark.internal.Logging.outputLogger", "SLF4J");
            
            JavaSparkContext jsc = new JavaSparkContext(sparkConf);
            
            // 恢复原始类加载器
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            return jsc;
        } catch (Exception e) {
            logger.error("创建JavaSparkContext失败", e);
            return null;
        }
    }

    @Bean
    @Conditional(SparkEnabledCondition.class)
    public SparkSession sparkSession(SparkConf sparkConf) {
        if (sparkConf == null || !sparkEnabled) {
            logger.info("Spark未启用或SparkConf为null，返回null SparkSession");
            return null;
        }
        
        logger.info("初始化SparkSession");
        try {
            // 确保使用适当的类加载器
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            
            // 在创建SparkSession前再次确保日志系统已配置
            System.setProperty("org.apache.spark.internal.Logging.outputLogger", "SLF4J");
            
            SparkSession session = SparkSession.builder()
                    .config(sparkConf)
                    .appName(appName)
                    .getOrCreate();
            
            // 恢复原始类加载器
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            return session;
        } catch (Exception e) {
            logger.error("SparkSession初始化失败", e);
            return null;
        }
    }
} 