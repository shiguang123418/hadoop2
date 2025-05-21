# Hadoop2-Kafka-WebSocket 模块

## Kryo序列化器配置说明

为解决Java 8 Lambda表达式在Spark中的序列化问题，本模块采用了Kryo序列化器进行优化。

### 主要改进

1. **使用Kryo序列化器**：
   - 替代默认的Java序列化器，提高序列化性能
   - 显式注册所有相关类，避免序列化问题
   - 特别处理Java 8 Lambda表达式和SerializedLambda类

2. **增强的类注册系统**：
   - 完善的KafkaKryoRegistrator类，负责注册所有需要序列化的类
   - 使用反射动态注册内部类和匿名类
   - 注册Java 8函数式接口、Stream API相关类

3. **优化的Spark配置**：
   - 调整Kryo缓冲区大小，支持大对象序列化
   - 配置类加载优先级，解决类加载冲突
   - 通过系统属性控制Lambda优化

### 配置说明

在`application.yml`中可以调整以下关键配置项：

```yaml
spark:
  # 是否使用Kryo序列化器
  kryo:
    enabled: true
    registrator: org.shiguang.spark.KafkaKryoRegistrator
  # Kryo缓冲区最大大小(MB)
  kryoserializer:
    buffer:
      max:
        mb: 64
```

### 序列化问题解决方案

本模块通过以下方式解决了Java 8 Lambda表达式在Spark中的序列化问题：

1. **显式注册SerializedLambda类**：
   ```java
   kryo.register(java.lang.invoke.SerializedLambda.class, new JavaSerializer());
   ```

2. **为Lambda相关类使用Java序列化器**：
   ```java
   kryo.addDefaultSerializer(SerializedLambda.class, JavaSerializer.class);
   ```

3. **注册所有函数式接口**：
   ```java
   kryo.register(Function.class);
   kryo.register(Consumer.class);
   kryo.register(Predicate.class);
   // 更多...
   ```

4. **使用静态内部类替代匿名内部类和Lambda表达式**：
   - ProcessRecordsFunction代替匿名处理函数
   - ExtractValueFunction代替map操作中的Lambda
   - WebSocketClient作为独立可序列化类

5. **使用广播变量**：
   对于不需要序列化传输但需要在执行器上使用的对象（如AnomalyDetector），使用Spark广播变量：
   ```java
   broadcastDetector = streamingContext.sparkContext().broadcast(anomalyDetector);
   ```

### 使用方法

1. 在代码中避免使用Lambda表达式，使用继承自函数接口的静态内部类
2. 确保所有传递给Spark的对象都是可序列化的
3. 为不可序列化字段使用transient关键字
4. 需序列化的自定义类都应该在KafkaKryoRegistrator中注册

### 潜在问题排查

如果仍然遇到序列化问题，请检查：

1. 是否有使用Lambda表达式的地方未替换为静态内部类
2. 是否有类未在KafkaKryoRegistrator中注册
3. 是否有不可序列化的对象未标记为transient
4. 检查类路径和类加载顺序是否正确 