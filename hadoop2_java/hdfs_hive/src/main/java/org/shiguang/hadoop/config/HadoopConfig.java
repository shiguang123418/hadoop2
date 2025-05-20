package org.shiguang.hadoop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.apache.hadoop.fs.FileSystem;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Hadoop相关配置类
 */
@Configuration
@PropertySource(value = "classpath:hadoop-config.properties", ignoreResourceNotFound = true)
public class HadoopConfig {

    @Value("${hadoop.hdfs.uri}")
    private String hdfsUri;

    @Value("${hadoop.hdfs.user:hadoop}")
    private String hdfsUser;

    @Value("${hadoop.hdfs.impl:org.apache.hadoop.hdfs.DistributedFileSystem}")
    private String hdfsImpl;

    @Value("${hadoop.hdfs.use.datanode.hostname:true}")
    private boolean useDatanodeHostname;

    @Value("${hadoop.hdfs.buffer.size:4096}")
    private int bufferSize;

    @Value("${hadoop.hdfs.replication:3}")
    private short replication;

    @Value("${hadoop.hdfs.block.size:67108864}")
    private long blockSize; // 默认64MB

    @Value("${hive.url:jdbc:hive2://192.168.1.192:10000}")
    private String hiveUrl;

    @Value("${hive.driver:org.apache.hive.jdbc.HiveDriver}")
    private String hiveDriver;

    @Value("${hive.username:}")
    private String hiveUsername;

    @Value("${hive.password:}")
    private String hivePassword;

    @Value("${hive.connection.timeout:30}")
    private int hiveConnectionTimeout;

    /**
     * 创建Hadoop配置
     * @return Hadoop Configuration对象
     */
    @Bean
    public org.apache.hadoop.conf.Configuration hadoopConfiguration() {
        org.apache.hadoop.conf.Configuration configuration = new org.apache.hadoop.conf.Configuration();
        
        // 基本配置
        configuration.set("fs.defaultFS", hdfsUri);
        configuration.set("dfs.client.use.datanode.hostname", String.valueOf(useDatanodeHostname));
        configuration.set("fs.hdfs.impl", hdfsImpl);
        
        // 性能相关配置
        configuration.setInt("io.file.buffer.size", bufferSize);
        configuration.setInt("dfs.replication", replication);
        configuration.setLong("dfs.blocksize", blockSize);
        
        // 可以在这里添加更多Hadoop配置
        
        return configuration;
    }

    /**
     * 创建HDFS FileSystem实例
     * @return FileSystem对象
     * @throws IOException 如果发生I/O错误
     * @throws URISyntaxException 如果URI语法错误
     * @throws InterruptedException 如果线程被中断
     */
    @Bean
    public FileSystem fileSystem() throws IOException, URISyntaxException, InterruptedException {
        try {
        return FileSystem.get(new URI(hdfsUri), hadoopConfiguration(), hdfsUser);
        } catch (IOException | URISyntaxException | InterruptedException e) {
            System.err.println("创建HDFS FileSystem失败: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * 获取Hive连接配置
     * @return Hive配置映射
     */
    @Bean
    public Map<String, String> hiveConnectionProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("url", hiveUrl);
        properties.put("driver", hiveDriver);
        properties.put("username", hiveUsername);
        properties.put("password", hivePassword);
        properties.put("connectionTimeout", String.valueOf(hiveConnectionTimeout));
        return properties;
    }
} 