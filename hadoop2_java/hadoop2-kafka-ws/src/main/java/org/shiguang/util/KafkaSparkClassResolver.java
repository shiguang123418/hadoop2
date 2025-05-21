package org.shiguang.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka和Spark集成时的类加载解析器
 * <p>
 * 用于解决分布式环境中Spark任务序列化和类加载问题
 * </p>
 */
public class KafkaSparkClassResolver implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(KafkaSparkClassResolver.class);
    
    /**
     * 配置任务的序列化相关设置
     * 
     * @param configMap 配置映射
     * @return 添加了序列化配置的映射
     */
    public static Map<String, String> getSerializationConfig(Map<String, String> configMap) {
        if (configMap == null) {
            configMap = new HashMap<>();
        }
        
        // 使用Java序列化器 - 更稳定但性能较差
        configMap.put("spark.serializer", "org.apache.spark.serializer.JavaSerializer");
        
        // 禁用Spark中使用Java 8 lambda表达式的优化
        configMap.put("spark.executor.extraJavaOptions", "-Dscala.usejavacp=true");
        configMap.put("spark.driver.extraJavaOptions", "-Dscala.usejavacp=true");
        
        // 确保类路径优先级正确
        configMap.put("spark.driver.userClassPathFirst", "true");
        configMap.put("spark.executor.userClassPathFirst", "true");
        
        return configMap;
    }
    
    /**
     * 设置当前线程的类加载器为当前类的类加载器
     */
    public static void setContextClassLoader() {
        Thread.currentThread().setContextClassLoader(KafkaSparkClassResolver.class.getClassLoader());
        logger.debug("已设置当前线程的类加载器为: {}", 
                Thread.currentThread().getContextClassLoader().getClass().getName());
    }
    
    /**
     * 创建一个序列化安全的对象传递机制
     * 
     * @param <T> 对象类型
     */
    public static class SerializableSingleton<T> implements Serializable {
        private static final long serialVersionUID = 1L;
        private transient T instance;
        private final Class<T> clazz;
        
        public SerializableSingleton(Class<T> clazz) {
            this.clazz = clazz;
        }
        
        public T get() {
            if (instance == null) {
                try {
                    instance = clazz.newInstance();
                } catch (Exception e) {
                    logger.error("无法创建类 {} 的实例", clazz.getName(), e);
                }
            }
            return instance;
        }
    }
} 