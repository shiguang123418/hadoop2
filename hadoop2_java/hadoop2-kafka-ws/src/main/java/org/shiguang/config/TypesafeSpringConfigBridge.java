package org.shiguang.config;

import com.typesafe.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.util.Properties;

/**
 * Typesafe配置与Spring配置的桥接类
 * 确保两个配置系统能够互相访问
 */
@Configuration
public class TypesafeSpringConfigBridge {

    @Autowired
    private Config typesafeConfig;

    @Autowired
    private Environment environment;

    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * 创建一个属性源配置器，将Typesafe Config的配置项添加到Spring环境中
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
    
    /**
     * 创建Properties对象，包含应用程序的关键配置项
     */
    @Bean(name = "applicationProperties")
    public Properties applicationProperties() {
        Properties props = new Properties();
        
        // 从Typesafe Config读取一些关键配置，并使其在Spring环境中可用
        if (typesafeConfig.hasPath("spark.master")) {
            props.setProperty("spark.master", typesafeConfig.getString("spark.master"));
        }
        
        if (typesafeConfig.hasPath("kafka.bootstrap-servers")) {
            props.setProperty("kafka.bootstrap.servers", 
                             typesafeConfig.getString("kafka.bootstrap-servers"));
        }
        
        return props;
    }
} 