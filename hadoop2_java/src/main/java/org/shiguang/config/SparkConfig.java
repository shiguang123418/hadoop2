package org.shiguang.config;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

@Configuration
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
        
        // 配置Spark关键参数
        System.setProperty("spark.driver.allowMultipleContexts", "true");
        System.setProperty("spark.driver.userClassPathFirst", "true");
        System.setProperty("spark.executor.userClassPathFirst", "true");
        
        // 禁用Kafka消费者缓存，避免与Spring Boot类加载器冲突
        System.setProperty("spark.kafka.consumer.cache.enabled", "false");
    }

    @PostConstruct
    public void init() {
        // 在Bean初始化时设置属性
        logger.info("初始化SparkConfig，设置日志系统...");
        
        // 自动寻找Kafka相关JAR并添加到类路径
        tryAddKafkaJarsToClasspath();
    }

    private static final Logger logger = LoggerFactory.getLogger(SparkConfig.class);

    @Value("${spark.app.name:农业数据分析Spark应用}")
    private String appName;

    @Value("${spark.master:local[2]}")
    private String masterUri;
    
    @Value("${spark.enabled:true}")
    private boolean sparkEnabled;

    @Bean
    public SparkConf sparkConf() {
        if (!sparkEnabled) {
            logger.info("Spark未启用，返回null SparkConf");
            return null;
        }
        
        logger.info("初始化Spark配置: app={}, master={}", appName, masterUri);
        
        SparkConf conf = new SparkConf()
                .setAppName(appName)
                .setMaster(masterUri)
                .set("spark.driver.allowMultipleContexts", "true")
                .set("spark.ui.enabled", "false")
                .set("spark.driver.userClassPathFirst", "true") 
                .set("spark.executor.userClassPathFirst", "true")
                .set("spark.hadoop.fs.permissions.umask-mode", "022")
                // 确保序列化正确
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                // 关闭Kafka消费者缓存
                .set("spark.kafka.consumer.cache.enabled", "false");
                
        // 尝试添加依赖中的Spark Streaming Kafka库到类路径
        try {
            String sparkVersion = org.apache.spark.package$.MODULE$.SPARK_VERSION();
            conf.set("spark.jars.packages", "org.apache.spark:spark-streaming-kafka-0-10_2.12:" + sparkVersion);
            logger.info("添加spark-streaming-kafka-0-10_2.12:{} 依赖", sparkVersion);
        } catch (Exception e) {
            logger.warn("无法获取Spark版本号，未能自动添加Kafka依赖: {}", e.getMessage());
        }
        
        return conf;
    }

    @Bean
    public SparkSession sparkSession(SparkConf sparkConf) {
        if (sparkConf == null || !sparkEnabled) {
            logger.info("Spark未启用或SparkConf为null，返回null SparkSession");
            return null;
        }
        
        logger.info("初始化SparkSession");
        try {
            // 尝试预加载关键类
            preloadKafkaClasses();
            
            // 确保使用适当的类加载器
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            
            SparkSession session = SparkSession.builder()
                    .config(sparkConf)
                    .appName(appName)
                    .getOrCreate();
            
            // 恢复原始类加载器
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            return session;
        } catch (Exception e) {
            logger.error("SparkSession初始化失败: {}", e.getMessage(), e);
            // 打印完整堆栈以便调试
            e.printStackTrace();
            return null;
        }
    }

    @Bean
    public JavaSparkContext javaSparkContext(SparkSession sparkSession) {
        if (sparkSession == null || !sparkEnabled) {
            logger.info("Spark未启用或SparkSession为null，返回null JavaSparkContext");
            return null;
        }
        
        logger.info("从SparkSession获取JavaSparkContext");
        try {
            // 确保Kafka类加载正确
            ensureKafkaClassesLoaded();
            
            // 直接从SparkSession获取JavaSparkContext，避免创建多个SparkContext
            return JavaSparkContext.fromSparkContext(sparkSession.sparkContext());
        } catch (Exception e) {
            logger.error("获取JavaSparkContext失败", e);
            return null;
        }
    }
    
    /**
     * 尝试将Kafka相关JAR添加到类路径
     */
    private void tryAddKafkaJarsToClasspath() {
        try {
            // 检查target/explicit-jars目录是否存在Kafka相关JAR
            File explicitJarsDir = new File("target/explicit-jars");
            if (explicitJarsDir.exists() && explicitJarsDir.isDirectory()) {
                File[] kafkaJars = explicitJarsDir.listFiles((dir, name) -> 
                    name.contains("spark-streaming-kafka") || name.contains("kafka-clients"));
                
                if (kafkaJars != null && kafkaJars.length > 0) {
                    logger.info("找到 {} 个Kafka相关JAR文件在target/explicit-jars目录中", kafkaJars.length);
                    
                    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
                    URLClassLoader urlClassLoader = new URLClassLoader(
                        Arrays.stream(kafkaJars).map(file -> {
                            try {
                                return file.toURI().toURL();
                            } catch (Exception e) {
                                logger.error("无法转换文件到URL: {}", file, e);
                                return null;
                            }
                        }).filter(url -> url != null).toArray(URL[]::new),
                        currentClassLoader
                    );
                    
                    Thread.currentThread().setContextClassLoader(urlClassLoader);
                    logger.info("已将Kafka JAR添加到类路径");
                } else {
                    logger.warn("未在target/explicit-jars目录中找到Kafka相关JAR文件");
                }
            }
        } catch (Exception e) {
            logger.error("添加Kafka JAR到类路径时出错", e);
        }
    }
    
    /**
     * 预加载关键Kafka类
     */
    private void preloadKafkaClasses() {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            
            // 尝试加载关键类
            String[] classNames = {
                "org.apache.spark.streaming.kafka010.KafkaRDDPartition",
                "org.apache.spark.streaming.kafka010.KafkaUtils",
                "org.apache.kafka.clients.consumer.KafkaConsumer"
            };
            
            for (String className : classNames) {
                try {
                    Class.forName(className, true, classLoader);
                    logger.info("预加载类成功: {}", className);
                } catch (ClassNotFoundException e) {
                    logger.warn("预加载类失败: {}, 错误: {}", className, e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("预加载Kafka类时出错", e);
        }
    }
    
    /**
     * 确保Spark Streaming Kafka 0-10相关类被正确加载
     * 这个方法尝试加载关键类，以防止ClassNotFoundException
     */
    private void ensureKafkaClassesLoaded() {
        try {
            logger.info("正在确保Spark Streaming Kafka类可以被正确加载");
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            
            // 尝试加载关键类
            String[] kafkaClasses = {
                "org.apache.spark.streaming.kafka010.KafkaRDDPartition",
                "org.apache.spark.streaming.kafka010.KafkaUtils",
                "org.apache.spark.streaming.kafka010.LocationStrategies",
                "org.apache.spark.streaming.kafka010.ConsumerStrategies",
                "org.apache.spark.streaming.kafka010.KafkaConsumer"
            };
            
            for (String className : kafkaClasses) {
                try {
                    Class.forName(className, true, classLoader);
                    logger.info("成功加载类: {}", className);
                } catch (ClassNotFoundException e) {
                    logger.warn("无法加载类: {}, 原因: {}", className, e.getMessage());
                    
                    // 尝试使用其他类加载器
                    try {
                        Class.forName(className, true, SparkConfig.class.getClassLoader());
                        logger.info("使用SparkConfig类加载器成功加载类: {}", className);
                    } catch (ClassNotFoundException e2) {
                        logger.warn("使用SparkConfig类加载器仍无法加载类: {}", className);
                    }
                }
            }
            
            // 输出当前类路径信息
            String classPath = System.getProperty("java.class.path");
            logger.info("当前类路径信息: {}", classPath);
        } catch (Exception e) {
            logger.error("检查Kafka类加载时出错", e);
        }
    }
} 