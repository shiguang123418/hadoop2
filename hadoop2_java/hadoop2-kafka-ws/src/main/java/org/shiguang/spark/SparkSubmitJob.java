package org.shiguang.spark;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.shiguang.spark.anomaly.AnomalyDetector;
import org.shiguang.spark.anomaly.ThresholdAnomalyDetector;
import org.shiguang.spark.sink.WebSocketSinkProvider;
import org.shiguang.util.KafkaSparkClassResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Serializable;
import java.net.URI;
import java.util.*;

/**
 * 独立的Spark作业，用于在JVM内部运行
 * 处理Kafka中的传感器数据并发送到WebSocket服务器
 * 采用Kryo序列化器提高性能并解决序列化问题
 */
public class SparkSubmitJob implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SparkSubmitJob.class);
    
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("用法: SparkSubmitJob <kafka-bootstrap-servers> <kafka-topic> <websocket-url>");
            System.exit(1);
        }
        
        String bootstrapServers = args[0];
        String topic = args[1];
        String webSocketUrl = args[2];
        
        SparkSubmitJob job = new SparkSubmitJob();
        job.run(bootstrapServers, topic, webSocketUrl, "local[*]", "AgricultureSensorProcessor", true, 64);
    }
    
    /**
     * 运行Spark作业
     * @param bootstrapServers Kafka服务器地址
     * @param topic Kafka主题
     * @param webSocketUrl WebSocket服务器URL
     * @param master Spark master URL
     * @param appName 应用程序名称
     * @param useKryo 是否使用Kryo序列化器
     * @param kryoBufferMaxSize Kryo缓冲区最大大小(MB)
     */
    public void run(String bootstrapServers, String topic, String webSocketUrl, 
                   String master, String appName, boolean useKryo, int kryoBufferMaxSize) {
        try {
            // 创建Spark配置
            SparkConf sparkConf = new SparkConf()
                    .setAppName(appName);
            
            // 如果没有设置master，则使用本地模式
            if (master != null && !master.isEmpty()) {
                sparkConf.setMaster(master);
            } else if (!sparkConf.contains("spark.master")) {
                sparkConf.setMaster("local[2]");
            }
            
            // 配置Kryo序列化器
            if (useKryo) {
                configureKryoSerializer(sparkConf, kryoBufferMaxSize);
            } else {
                // 回退到Java序列化器
                sparkConf.set("spark.serializer", "org.apache.spark.serializer.JavaSerializer");
                sparkConf.set("spark.serializer.objectStreamReset", "100");
            }
            
            // 应用KafkaSparkClassResolver的通用配置
            Map<String, String> serializationConfig = KafkaSparkClassResolver.getSerializationConfig(new HashMap<>());
            for (Map.Entry<String, String> entry : serializationConfig.entrySet()) {
                sparkConf.set(entry.getKey(), entry.getValue());
            }
            
            // 设置类加载优先级
            sparkConf.set("spark.driver.userClassPathFirst", "true");
            sparkConf.set("spark.executor.userClassPathFirst", "true");
            
            // 禁用推测性执行，提高稳定性
            sparkConf.set("spark.speculation", "false");
            
            // 创建流上下文
            JavaStreamingContext jssc = new JavaStreamingContext(
                    sparkConf, Durations.seconds(5));
            
            // 创建Kafka直接流
            Map<String, Object> kafkaParams = new HashMap<>();
            kafkaParams.put("bootstrap.servers", bootstrapServers);
            kafkaParams.put("key.deserializer", StringDeserializer.class);
            kafkaParams.put("value.deserializer", StringDeserializer.class);
            kafkaParams.put("group.id", "agriculture-sensor-group-" + System.currentTimeMillis());
            kafkaParams.put("auto.offset.reset", "latest");
            kafkaParams.put("enable.auto.commit", false);
            
            Collection<String> topics = Collections.singletonList(topic);
            
            JavaInputDStream<ConsumerRecord<String, String>> stream =
                    KafkaUtils.createDirectStream(
                            jssc,
                            LocationStrategies.PreferConsistent(),
                            ConsumerStrategies.<String, String>Subscribe(topics, kafkaParams)
                    );
            
            // 处理流
            stream.foreachRDD(new ProcessRecordsFunction(webSocketUrl));
            
            // 启动流处理
            jssc.start();
            jssc.awaitTermination();
            
        } catch (Exception e) {
            logger.error("运行Spark作业出错", e);
            throw new RuntimeException("运行Spark作业失败", e);
        }
    }
    
    /**
     * 配置Kryo序列化器
     * @param sparkConf Spark配置
     * @param kryoBufferMaxSize Kryo缓冲区最大大小(MB)
     */
    private void configureKryoSerializer(SparkConf sparkConf, int kryoBufferMaxSize) {
        logger.info("配置Kryo序列化器，缓冲区最大大小: {}MB", kryoBufferMaxSize);
        
        // 设置Kryo序列化器
        sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
        
        // 注册自定义Kryo注册器
        sparkConf.set("spark.kryo.registrator", "org.shiguang.spark.KafkaKryoRegistrator");
        
        // Kryo设置
        sparkConf.set("spark.kryo.registrationRequired", "false");  // 不要求所有类都注册，可能会影响性能但提高兼容性
        sparkConf.set("spark.kryoserializer.buffer", "32k");  // 初始缓冲区大小
        sparkConf.set("spark.kryoserializer.buffer.max", kryoBufferMaxSize + "m");  // 最大缓冲区大小
        
        // 注册Java 8类
        sparkConf.registerKryoClasses(new Class<?>[] {
            java.util.function.Function.class,
            java.util.function.Consumer.class,
            java.util.function.Predicate.class,
            java.util.function.Supplier.class,
            java.util.stream.Stream.class,
            java.util.stream.BaseStream.class,
            java.util.Optional.class,
            java.util.OptionalInt.class,
            java.util.OptionalLong.class,
            java.util.OptionalDouble.class,
            java.util.Collections.class,
            java.lang.invoke.SerializedLambda.class // 关键：注册SerializedLambda类
        });
    }
    
    /**
     * 处理记录的函数类
     */
    private static class ProcessRecordsFunction implements VoidFunction<JavaRDD<ConsumerRecord<String, String>>>, Serializable {
        private static final long serialVersionUID = 1L;
        private final String webSocketUrl;
        
        public ProcessRecordsFunction(String webSocketUrl) {
            this.webSocketUrl = webSocketUrl;
        }
        
        @Override
        public void call(JavaRDD<ConsumerRecord<String, String>> rdd) throws Exception {
            if (rdd.isEmpty()) {
                return;
            }
            
            // 提取值
            List<String> jsonStrings = rdd.map(new ExtractValueFunction()).collect();
            
            // 处理JSON数据
            List<Map<String, Object>> processedData = processJsonData(jsonStrings);
            
            // 发送到WebSocket
            sendToWebSocket(processedData, webSocketUrl);
        }
        
        /**
         * 处理JSON数据
         */
        private List<Map<String, Object>> processJsonData(List<String> jsonStrings) {
            List<Map<String, Object>> results = new ArrayList<>();
            
            for (String json : jsonStrings) {
                try {
                    // 解析JSON
                    JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                    
                    // 验证和提取字段
                    if (validateJsonFields(jsonObject)) {
                        Map<String, Object> sensorData = new HashMap<>();
                        
                        // 提取基本字段
                        String sensorId = jsonObject.get("sensor_id").getAsString();
                        String sensorType = jsonObject.get("sensor_type").getAsString();
                        double value = jsonObject.get("value").getAsDouble();
                        String timestamp = jsonObject.get("timestamp").getAsString();
                        String unit = jsonObject.has("unit") ? jsonObject.get("unit").getAsString() : "";
                        String location = jsonObject.has("location") ? jsonObject.get("location").getAsString() : "";
                        String farmId = jsonObject.has("farm_id") ? jsonObject.get("farm_id").getAsString() : "";
                        String cropType = jsonObject.has("crop_type") ? jsonObject.get("crop_type").getAsString() : "";
                        String region = jsonObject.has("region") ? jsonObject.get("region").getAsString() : "";
                        
                        // 填充数据
                        sensorData.put("sensor_id", sensorId);
                        sensorData.put("sensor_type", sensorType);
                        sensorData.put("value", value);
                        sensorData.put("timestamp", timestamp);
                        sensorData.put("unit", unit);
                        sensorData.put("location", location);
                        sensorData.put("farm_id", farmId);
                        sensorData.put("crop_type", cropType);
                        sensorData.put("region", region);
                        
                        // 检测异常 - 简单阈值
                        boolean isAnomaly = detectAnomaly(sensorType, value);
                        sensorData.put("is_anomaly", isAnomaly);
                        
                        results.add(sensorData);
                    }
                } catch (Exception e) {
                    logger.error("解析JSON出错: {} for JSON: {}", e.getMessage(), json, e);
                }
            }
            
            return results;
        }
        
        /**
         * 简单的异常检测
         */
        private boolean detectAnomaly(String sensorType, double value) {
            // 简单的阈值检测，实际应用中应该根据不同传感器类型使用不同阈值
            double threshold = 0;
            
            switch (sensorType.toLowerCase()) {
                case "temperature":
                    threshold = 35.0; // 温度超过35度异常
                    break;
                case "humidity":
                    threshold = 90.0; // 湿度超过90%异常
                    break;
                case "soil_moisture":
                    threshold = 85.0; // 土壤湿度超过85%异常
                    break;
                default:
                    threshold = 100.0;
            }
            
            return Math.abs(value) > threshold;
        }
        
        /**
         * 验证JSON字段
         */
        private boolean validateJsonFields(JsonObject jsonObject) {
            try {
                return jsonObject.has("sensor_id") && 
                       jsonObject.has("sensor_type") && 
                       jsonObject.has("value") && 
                       jsonObject.has("timestamp");
            } catch (Exception e) {
                logger.error("验证JSON字段失败", e);
                return false;
            }
        }
        
        /**
         * 发送数据到WebSocket
         */
        private void sendToWebSocket(List<Map<String, Object>> data, String webSocketUrl) {
            if (data.isEmpty()) {
                return;
            }
            
            try {
                WebSocketClient client = new WebSocketClient(new URI(webSocketUrl));
                client.connect();
                
                // 等待连接建立
                int attempts = 0;
                while (!client.isOpen() && attempts < 10) {
                    Thread.sleep(100);
                    attempts++;
                }
                
                if (client.isOpen()) {
                    Gson gson = new Gson();
                    for (Map<String, Object> item : data) {
                        String json = gson.toJson(item);
                        client.send(json);
                    }
                    
                    logger.info("发送了 {} 条消息到WebSocket", data.size());
                } else {
                    logger.error("无法连接到WebSocket服务器: {}", webSocketUrl);
                }
                
                client.close();
            } catch (Exception e) {
                logger.error("发送数据到WebSocket出错", e);
            }
        }
    }
    
    /**
     * 从ConsumerRecord中提取值的函数
     */
    private static class ExtractValueFunction implements Function<ConsumerRecord<String, String>, String>, Serializable {
        private static final long serialVersionUID = 1L;
        
        @Override
        public String call(ConsumerRecord<String, String> record) {
            return record.value();
        }
    }
    
    /**
     * 简单的WebSocket客户端实现
     */
    private static class WebSocketClient implements Serializable {
        private static final long serialVersionUID = 1L;
        private final URI serverUri;
        private transient java.net.Socket socket;
        private transient java.io.PrintWriter writer;
        
        public WebSocketClient(URI serverUri) {
            this.serverUri = serverUri;
        }
        
        public void connect() {
            try {
                String host = serverUri.getHost();
                int port = serverUri.getPort() > 0 ? serverUri.getPort() : 80;
                
                socket = new java.net.Socket(host, port);
                writer = new java.io.PrintWriter(
                    new java.io.OutputStreamWriter(socket.getOutputStream(), "UTF-8"), 
                    true
                );
                
                // 发送WebSocket握手请求
                writer.println("GET " + serverUri.getPath() + " HTTP/1.1");
                writer.println("Host: " + host + ":" + port);
                writer.println("Upgrade: websocket");
                writer.println("Connection: Upgrade");
                writer.println("Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ=="); // 固定的Key用于简单测试
                writer.println("Sec-WebSocket-Version: 13");
                writer.println("Origin: http://" + host + ":" + port);
                writer.println("");
                writer.flush();
                
                // 实际应用应该解析响应，但这里简化处理
                Thread.sleep(500); // 给服务器一点时间响应
                
                logger.debug("已连接到WebSocket服务器: {}", serverUri);
            } catch (Exception e) {
                logger.error("连接到WebSocket服务器失败: {}", serverUri, e);
            }
        }
        
        public boolean isOpen() {
            return socket != null && socket.isConnected() && !socket.isClosed();
        }
        
        public void send(String message) {
            if (isOpen() && writer != null) {
                // 非常简化的WebSocket消息发送，实际应用需要按照WebSocket协议格式化帧
                writer.println(message);
                writer.flush();
            }
        }
        
        public void close() {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                logger.debug("已关闭WebSocket连接");
            } catch (Exception e) {
                logger.error("关闭WebSocket连接失败", e);
            }
        }
    }
} 