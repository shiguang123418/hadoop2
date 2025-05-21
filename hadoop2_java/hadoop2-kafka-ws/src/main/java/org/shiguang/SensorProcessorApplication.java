package org.shiguang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 农业传感器数据处理应用程序主类
 * <p>
 * 该应用程序是一个基于Spring Boot的服务，用于处理来自农业传感器的实时数据流。
 * 它使用Kafka作为消息队列，Spark Streaming进行数据处理，以及WebSocket向前端客户端
 * 实时推送处理后的数据。应用主要功能包括：
 * </p>
 * <ul>
 *   <li>从Kafka主题订阅并消费农业传感器数据</li>
 *   <li>使用Spark Streaming进行实时数据处理和分析</li>
 *   <li>检测传感器数据中的异常值</li>
 *   <li>通过WebSocket将处理后的数据推送给客户端</li>
 *   <li>提供REST API用于控制流处理服务的启停</li>
 * </ul>
 */
@SpringBootApplication
public class SensorProcessorApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SensorProcessorApplication.class, args);
    }
} 