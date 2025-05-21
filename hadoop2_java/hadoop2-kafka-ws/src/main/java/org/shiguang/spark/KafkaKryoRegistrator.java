package org.shiguang.spark;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.spark.serializer.KryoRegistrator;
import org.apache.spark.streaming.kafka010.KafkaRDDPartition;
import org.apache.spark.streaming.kafka010.KafkaRDD;
import org.apache.spark.streaming.kafka010.OffsetRange;
import org.shiguang.spark.anomaly.AnomalyDetector;
import org.shiguang.spark.anomaly.ThresholdAnomalyDetector;
import org.shiguang.spark.processor.SensorProcessor;
import org.shiguang.spark.sink.WebSocketSinkProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.*;

/**
 * Kafka相关类的Kryo注册器
 * <p>
 * 用于在分布式环境中确保Kafka相关类和应用程序类能够被正确序列化
 * 增强了对Java 8 Lambda表达式和相关类的支持
 * </p>
 */
public class KafkaKryoRegistrator implements KryoRegistrator {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaKryoRegistrator.class);
    
    @Override
    public void registerClasses(Kryo kryo) {
        try {
            logger.info("注册Kafka和应用程序相关类到Kryo序列化器");
            
            // 设置Kryo序列化器选项
            kryo.setInstantiatorStrategy(new org.objenesis.strategy.StdInstantiatorStrategy());
            kryo.setRegistrationRequired(false); // 允许未注册类使用默认序列化器
            kryo.setReferences(true); // 启用引用跟踪，避免循环引用问题
            
            // 为特定类使用Java序列化器，确保兼容性
            kryo.addDefaultSerializer(SerializedLambda.class, JavaSerializer.class);
            
            // 使用兼容字段序列化器作为默认序列化器
            kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
            
            // 注册Kafka和Spark Streaming Kafka相关的类
            kryo.register(KafkaRDDPartition.class);
            kryo.register(KafkaRDD.class);
            kryo.register(ConsumerRecord.class);
            kryo.register(OffsetRange.class);
            kryo.register(org.apache.kafka.common.TopicPartition.class);
            kryo.register(org.apache.kafka.clients.consumer.ConsumerRecords.class);
            
            // 注册应用程序相关类
            kryo.register(AgricultureSensorProcessor.class);
            kryo.register(SparkSubmitJob.class);
            kryo.register(AnomalyDetector.class);
            kryo.register(ThresholdAnomalyDetector.class);
            kryo.register(SensorProcessor.class);
            kryo.register(WebSocketSinkProvider.class);
            kryo.register(WebSocketSinkProvider.WebSocketForeachWriter.class);
            
            // 注册Spark处理函数相关类
            registerSparkProcessorClasses(kryo);
            
            // 注册JSON相关类
            kryo.register(Gson.class);
            kryo.register(JsonObject.class);
            
            // 注册匿名内部类
            registerAnonymousClasses(kryo);
            
            // 注册Java 8相关类
            registerJava8Classes(kryo);
            
            // 注册常用的集合类
            registerCollectionClasses(kryo);
            
            // 注册函数接口及其实现
            registerFunctionClasses(kryo);
            
            // 注册数组类型
            registerArrayClasses(kryo);
            
            logger.info("Kafka和应用程序相关类注册完成");
        } catch (Exception e) {
            logger.error("注册类失败", e);
        }
    }
    
    /**
     * 注册Spark处理器相关类
     */
    private void registerSparkProcessorClasses(Kryo kryo) {
        try {
            // 使用反射动态注册内部类，避免直接引用非可见类
            try {
                // 注册SparkSubmitJob中的内部类
                Class<?>[] submitJobClasses = SparkSubmitJob.class.getDeclaredClasses();
                if (submitJobClasses.length > 0) {
                    for (Class<?> cls : submitJobClasses) {
                        kryo.register(cls);
                        logger.debug("注册SparkSubmitJob内部类: {}", cls.getName());
                    }
                }
            } catch (Exception e) {
                logger.warn("注册SparkSubmitJob内部类失败: {}", e.getMessage());
            }
            
            // 使用反射获取内部类并注册，而不是直接引用
            try {
                for (Class<?> cls : AgricultureSensorProcessor.class.getDeclaredClasses()) {
                    kryo.register(cls);
                    logger.debug("注册AgricultureSensorProcessor内部类: {}", cls.getName());
                }
            } catch (Exception e) {
                logger.warn("注册AgricultureSensorProcessor内部类失败: {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.warn("注册Spark处理器类失败: {}", e.getMessage());
        }
    }
    
    /**
     * 注册Java 8相关类
     */
    private void registerJava8Classes(Kryo kryo) {
        // 注册Java 8 Lambda序列化相关类
        kryo.register(SerializedLambda.class, new JavaSerializer());
        kryo.register(ClosureSerializer.class);
        
        // 注册Java 8函数接口
        kryo.register(Function.class);
        kryo.register(BiFunction.class);
        kryo.register(Consumer.class);
        kryo.register(BiConsumer.class);
        kryo.register(Predicate.class);
        kryo.register(BiPredicate.class);
        kryo.register(Supplier.class);
        kryo.register(Runnable.class);
        kryo.register(Comparator.class);
        
        // 注册Java 8 Stream相关类 - 使用安全的方式，避免直接引用非可见类
        try {
            kryo.register(java.util.stream.Stream.class);
            kryo.register(java.util.stream.BaseStream.class);
            
            // 使用反射获取流管道类
            try {
                Class<?> referenceStreamClass = Class.forName("java.util.stream.ReferencePipeline");
                kryo.register(referenceStreamClass);
                logger.debug("注册ReferencePipeline类: {}", referenceStreamClass.getName());
                
                // 获取内部类
                for (Class<?> cls : referenceStreamClass.getDeclaredClasses()) {
                    kryo.register(cls);
                    logger.debug("注册Stream内部类: {}", cls.getName());
                }
            } catch (ClassNotFoundException e) {
                logger.warn("找不到Stream实现类: {}", e.getMessage());
            }
            
            kryo.register(java.util.stream.StreamSupport.class);
            kryo.register(java.util.stream.Collector.class);
            kryo.register(java.util.stream.Collectors.class);
        } catch (Exception e) {
            logger.warn("注册Stream类时出错: {}", e.getMessage());
        }
        
        // 注册Java 8 Optional相关类
        kryo.register(Optional.class);
        kryo.register(OptionalInt.class);
        kryo.register(OptionalLong.class);
        kryo.register(OptionalDouble.class);
    }
    
    /**
     * 注册集合类
     */
    private void registerCollectionClasses(Kryo kryo) {
        kryo.register(HashMap.class);
        kryo.register(ArrayList.class);
        kryo.register(LinkedList.class);
        kryo.register(HashSet.class);
        kryo.register(TreeMap.class);
        kryo.register(TreeSet.class);
        kryo.register(LinkedHashMap.class);
        kryo.register(LinkedHashSet.class);
        kryo.register(java.util.Collections.EMPTY_LIST.getClass());
        kryo.register(java.util.Collections.EMPTY_MAP.getClass());
        kryo.register(java.util.Collections.EMPTY_SET.getClass());
        kryo.register(java.util.Collections.singletonList(null).getClass());
        kryo.register(java.util.Collections.singleton(null).getClass());
        kryo.register(Collections.unmodifiableCollection(new ArrayList<>()).getClass());
        kryo.register(Collections.unmodifiableList(new ArrayList<>()).getClass());
        kryo.register(Collections.unmodifiableMap(new HashMap<>()).getClass());
        kryo.register(Collections.unmodifiableSet(new HashSet<>()).getClass());
        kryo.register(Map.Entry.class);
        kryo.register(java.util.AbstractMap.SimpleEntry.class);
        kryo.register(java.util.AbstractMap.SimpleImmutableEntry.class);
    }
    
    /**
     * 注册函数接口及其实现
     */
    private void registerFunctionClasses(Kryo kryo) {
        kryo.register(org.apache.spark.api.java.function.Function.class);
        kryo.register(org.apache.spark.api.java.function.Function2.class);
        kryo.register(org.apache.spark.api.java.function.VoidFunction.class);
        kryo.register(org.apache.spark.api.java.function.FlatMapFunction.class);
        kryo.register(org.apache.spark.api.java.function.PairFunction.class);
    }
    
    /**
     * 注册数组类型
     */
    private void registerArrayClasses(Kryo kryo) {
        kryo.register(String[].class);
        kryo.register(Object[].class);
        kryo.register(byte[].class);
        kryo.register(char[].class);
        kryo.register(int[].class);
        kryo.register(float[].class);
        kryo.register(double[].class);
        kryo.register(boolean[].class);
        kryo.register(long[].class);
        kryo.register(Class[].class);
    }
    
    /**
     * 注册匿名内部类
     */
    private void registerAnonymousClasses(Kryo kryo) {
        try {
            // 查找AgricultureSensorProcessor中的匿名内部类
            for (Class<?> cls : AgricultureSensorProcessor.class.getDeclaredClasses()) {
                kryo.register(cls);
            }
            
            // 查找WebSocketSinkProvider中的匿名内部类
            for (Class<?> cls : WebSocketSinkProvider.class.getDeclaredClasses()) {
                kryo.register(cls);
            }
            
            // 查找SparkSubmitJob中的匿名内部类
            for (Class<?> cls : SparkSubmitJob.class.getDeclaredClasses()) {
                kryo.register(cls);
            }
            
        } catch (Exception e) {
            logger.error("注册匿名内部类失败", e);
        }
    }
    
    /**
     * 闭包序列化器类，用于解决Java 8 Lambda序列化问题
     */
    private static class ClosureSerializer extends JavaSerializer {
        public ClosureSerializer() {
            super();
        }
    }
} 