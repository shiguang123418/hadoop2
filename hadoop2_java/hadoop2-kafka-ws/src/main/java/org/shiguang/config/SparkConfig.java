package org.shiguang.config;

import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
public class SparkConfig {
    
    @Autowired
    private AppProperties appProperties;

    @Bean
    public SparkConf sparkConf() {
        return new SparkConf()
            .setAppName("AgricultureSensorProcessor")
            .setMaster(appProperties.getSpark().getMaster())
            .set("spark.streaming.stopGracefullyOnShutdown", "true")
            .set("spark.ui.enabled", String.valueOf(appProperties.getSpark().getUi().isEnabled()))
            .set("spark.ui.port", String.valueOf(appProperties.getSpark().getUi().getPort()));
    }

    @Bean
    public JavaStreamingContext javaStreamingContext(SparkConf sparkConf) {
        JavaStreamingContext jssc = new JavaStreamingContext(
            sparkConf, Durations.seconds(appProperties.getSpark().getBatchInterval()));
        jssc.sparkContext().setLogLevel("WARN");
        return jssc;
    }
} 