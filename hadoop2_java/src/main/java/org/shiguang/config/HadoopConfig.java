package org.shiguang.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;

/**
 * Hadoop配置类
 */
@org.springframework.context.annotation.Configuration
@Conditional(HadoopEnabledCondition.class)
@Slf4j
public class HadoopConfig {

    @Value("${hadoop.config.fs.defaultFS}")
    private String defaultFS;
    
    @Value("${hadoop.config.dfs.replication:1}")
    private int replication;
    
    @Value("${hadoop.config.hdfs.user:root}")
    private String hdfsUser;
    
    @Autowired
    private Environment env;
    
    /**
     * 创建Hadoop配置对象
     */
    @Bean
    public Configuration hadoopConfiguration() {
        Configuration configuration = new Configuration();
        
        // 设置基本配置
        configuration.set("fs.defaultFS", defaultFS);
        configuration.set("dfs.replication", String.valueOf(replication));
        
        // 设置HDFS用户环境变量
        System.setProperty("HADOOP_USER_NAME", hdfsUser);
        
        // ======重要: 修复远程连接问题======
        // 1. 禁用主机名解析，使用IP直连
        configuration.setBoolean("dfs.client.use.datanode.hostname", false);
        configuration.setBoolean("dfs.datanode.use.datanode.hostname", false);
        configuration.setBoolean("dfs.client.datanode-address.use.hostnames", false);
        configuration.setBoolean("dfs.client.namenode-address.use.hostnames", false);
        
        // 2. 设置安全相关参数
        configuration.setBoolean("dfs.permissions.enabled", false);
        configuration.setBoolean("hadoop.security.token.service.use_ip", true);
        
        // 3. 设置网络和连接参数
        configuration.setInt("dfs.client.socket-timeout", 120000);
        configuration.setInt("ipc.client.connect.timeout", 120000);
        configuration.setInt("ipc.client.connect.max.retries", 10);
        configuration.setInt("ipc.client.connect.retry.interval", 5000);
        
        // 4. 块复制参数
        configuration.set("dfs.client.block.write.replace-datanode-on-failure.policy", "NEVER");
        configuration.setBoolean("dfs.client.block.write.replace-datanode-on-failure.enable", false);
        
        // 5. 禁用主机名
        configuration.setBoolean("dfs.client.resolved.remote.ips", true);
        
        // 6. 从配置文件加载其他参数
        loadAdditionalParameters(configuration);
        
        log.info("初始化Hadoop配置: fs.defaultFS={}, user={}, socket-timeout={}", 
                defaultFS, hdfsUser, configuration.get("dfs.client.socket-timeout"));
        return configuration;
    }
    
    /**
     * 加载配置文件中的其他参数
     */
    private void loadAdditionalParameters(Configuration configuration) {
        setPropertyIfExists(configuration, "fs.hdfs.impl", "hadoop.config.hdfs.impl");
        setPropertyIfExists(configuration, "fs.file.impl", "hadoop.config.file.impl");
        setPropertyIfExists(configuration, "hadoop.job.ugi", "hadoop.config.hadoop.job.ugi");
        setPropertyIfExists(configuration, "dfs.client.use.legacy.blockreader", "hadoop.config.dfs.client.use.legacy.blockreader");
        setPropertyIfExists(configuration, "dfs.support.append", "hadoop.config.dfs.support.append");
        setPropertyIfExists(configuration, "dfs.safemode.extension", "hadoop.config.dfs.safemode.extension");
        
        // 加载主机名映射配置
        loadHostMappings(configuration);
    }
    
    /**
     * 加载主机名映射
     */
    private void loadHostMappings(Configuration configuration) {
        try {
            // 主要服务器映射 (192.168.1.192 → 14.103.176.218)
            String prefix = "hadoop.config.dfs.client.resolved.remote.";
            String ipMapping = "14.103.176.218";
            
            // 添加主机名映射
            configuration.set("dfs.client.resolved.remote.shiguang", ipMapping);
            configuration.set("dfs.client.resolved.remote.192.168.1.192", ipMapping);
            configuration.set("dfs.client.resolved.remote.localhost", ipMapping);
            
            log.info("添加主机名映射: shiguang,192.168.1.192,localhost → {}", ipMapping);
            
            // 从配置中加载更多映射
            String shiguang = env.getProperty(prefix + "shiguang");
            if (shiguang != null) {
                configuration.set("dfs.client.resolved.remote.shiguang", shiguang);
            }
            
            // 这里可以添加更多的主机名映射
        } catch (Exception e) {
            log.warn("加载主机名映射时出错", e);
        }
    }
    
    /**
     * 从配置文件中加载属性
     */
    private void setPropertyIfExists(Configuration configuration, String hadoopKey, String springKey) {
        String value = env.getProperty(springKey);
        if (value != null) {
            configuration.set(hadoopKey, value);
            log.debug("加载Hadoop配置: {} = {}", hadoopKey, value);
        }
    }

    /**
     * 创建HDFS文件系统对象
     */
    @Bean
    public FileSystem fileSystem(Configuration configuration) throws IOException, URISyntaxException, InterruptedException {
        try {
            // 使用清晰的URI创建连接
            URI hdfsUri = new URI(defaultFS);
            
            // 尝试通过IP直连
            String host = hdfsUri.getHost();
            int port = hdfsUri.getPort();
            log.info("连接HDFS: {}:{} with user {}", host, port, hdfsUser);
            
            // 使用IP直连方式
            FileSystem fs = FileSystem.get(hdfsUri, configuration, hdfsUser);
            log.info("HDFS文件系统初始化成功: {}", fs.getUri());
            
            return fs;
        } catch (Exception e) {
            log.error("HDFS文件系统初始化失败", e);
            throw e;
        }
    }
}