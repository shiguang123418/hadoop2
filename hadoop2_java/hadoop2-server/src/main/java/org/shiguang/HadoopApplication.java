package org.shiguang;

import org.shiguang.utils.AppConstants;
import org.shiguang.module.hdfs.client.HDFSClient;
import org.shiguang.module.hive.client.HiveClient;
import org.shiguang.module.kafka.client.KafkaClient;
import org.shiguang.module.spark.client.SparkClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Hadoop服务应用程序入口
 */
@SpringBootApplication
@EnableScheduling
@ServletComponentScan
public class HadoopApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(HadoopApplication.class, args);
        
        String serverPort = context.getEnvironment().getProperty("server.port", "8080");
        String apiPath = context.getEnvironment().getProperty("server.servlet.context-path", "/api");
        String serverHost = context.getEnvironment().getProperty("app.server.host", "localhost");
        
        printApplicationBanner(serverHost, serverPort, apiPath);
        
        // 检查是否启用控制台交互模式
        boolean enableConsole = Arrays.asList(args).contains("--console");
        if (enableConsole) {
            startInteractiveConsole(context);
        }
    }
    
    /**
     * 打印应用启动信息
     */
    private static void printApplicationBanner(String serverHost, String serverPort, String apiPath) {
        System.out.println("====== " + AppConstants.APP_NAME + " 已启动 ======");
        System.out.println("REST API接口：");
        System.out.println("- HDFS API: http://" + serverHost + ":" + serverPort + apiPath + "/hdfs");
        System.out.println("- Hive API: http://" + serverHost + ":" + serverPort + apiPath + "/hive");
        System.out.println("- Kafka API: http://" + serverHost + ":" + serverPort + apiPath + "/kafka");
        System.out.println("- Spark API: http://" + serverHost + ":" + serverPort + apiPath + "/spark");
        System.out.println("- Auth API: http://" + serverHost + ":" + serverPort + apiPath + "/auth");
        System.out.println("WebSocket接口：");
        System.out.println("- Kafka实时消息: ws://" + serverHost + ":" + serverPort + "/ws/kafka");
    }
    
    /**
     * 启动交互式控制台
     * @param context Spring应用上下文
     */
    private static void startInteractiveConsole(ConfigurableApplicationContext context) {
        System.out.println("\n可用控制台服务：");
        System.out.println("1. HDFS文件操作");
        System.out.println("2. Hive查询");
        System.out.println("3. Kafka主题管理");
        System.out.println("4. Spark数据分析");
        System.out.println("0. 退出");
        
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        while (running) {
            System.out.print("\n请选择要使用的服务 (0-4): ");
            String choice = scanner.nextLine();
            
            try {
                switch (choice) {
                    case "1":
                        // HDFS操作示例
                        handleHdfsOperations(context, scanner);
                        break;
                        
                    case "2":
                        // Hive查询示例
                        handleHiveOperations(context, scanner);
                        break;
                        
                    case "3":
                        // Kafka操作示例
                        handleKafkaOperations(context, scanner);
                        break;
                        
                    case "4":
                        // Spark操作示例
                        handleSparkOperations(context, scanner);
                        break;
                        
                    case "0":
                        running = false;
                        System.out.println("退出程序");
                        break;
                        
                    default:
                        System.out.println("无效选择，请重新输入");
                }
            } catch (Exception e) {
                System.out.println("操作失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        scanner.close();
        context.close();
        System.exit(0);
    }
    
    /**
     * 处理HDFS操作
     */
    private static void handleHdfsOperations(ConfigurableApplicationContext context, Scanner scanner) {
        HDFSClient hdfsClient = context.getBean(HDFSClient.class);
        System.out.println("HDFS连接状态: " + (hdfsClient.getFileSystem() != null ? "已连接" : "未连接"));
        
        if (hdfsClient.getFileSystem() != null) {
            System.out.println("HDFS URI: " + hdfsClient.getFileSystem().getUri());
        } else {
            System.out.println("HDFS未连接，请检查配置");
            return;
        }
        
        System.out.println("\nHDFS操作菜单:");
        System.out.println("1. 列出目录内容");
        System.out.println("2. 创建目录");
        System.out.println("3. 上传文件");
        System.out.println("4. 下载文件");
        System.out.println("5. 删除文件/目录");
        System.out.println("0. 返回主菜单");
        
        System.out.print("\n请选择操作: ");
        String hdfsChoice = scanner.nextLine();
        
        try {
            switch (hdfsChoice) {
                case "1":
                    System.out.print("请输入要列出内容的目录路径: ");
                    String listPath = scanner.nextLine();
                    hdfsClient.listFiles(listPath).forEach(file -> {
                        System.out.printf("%-30s %-10s %-10s %s%n",
                                file.get("name"),
                                Boolean.TRUE.equals(file.get("isDirectory")) ? "<DIR>" : file.get("length") + " bytes",
                                file.get("owner"),
                                file.get("permission"));
                    });
                    break;
                    
                case "2":
                    System.out.print("请输入要创建的目录路径: ");
                    String dirPath = scanner.nextLine();
                    boolean success = hdfsClient.mkdir(dirPath);
                    System.out.println(success ? "目录创建成功" : "目录已存在");
                    break;
                    
                case "3":
                    System.out.print("请输入本地文件路径: ");
                    String localFile = scanner.nextLine();
                    System.out.print("请输入HDFS目标路径 (留空使用默认路径): ");
                    String targetPath = scanner.nextLine();
                    Map<String, Object> result = hdfsClient.uploadLocalFile(localFile, targetPath);
                    System.out.println("文件上传完成：" + result.get("path") + " (" + result.get("size") + " bytes)");
                    break;
                    
                case "4":
                    System.out.print("请输入HDFS文件路径: ");
                    String hdfsFile = scanner.nextLine();
                    System.out.print("请输入本地保存路径: ");
                    String localSavePath = scanner.nextLine();
                    hdfsClient.downloadFile(hdfsFile, localSavePath);
                    System.out.println("文件下载完成");
                    break;
                    
                case "5":
                    System.out.print("请输入要删除的路径: ");
                    String deletePath = scanner.nextLine();
                    System.out.print("是否递归删除 (y/n): ");
                    boolean recursive = "y".equalsIgnoreCase(scanner.nextLine());
                    boolean deleted = hdfsClient.delete(deletePath, recursive);
                    System.out.println(deleted ? "删除成功" : "删除失败");
                    break;
                
                case "0":
                    // 返回主菜单
                    break;
                    
                default:
                    System.out.println("暂未实现该功能，请选择其他操作");
            }
        } catch (Exception e) {
            System.out.println("操作失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 处理Hive操作
     */
    private static void handleHiveOperations(ConfigurableApplicationContext context, Scanner scanner) {
        try {
            HiveClient hiveClient = context.getBean(HiveClient.class);
            System.out.println("Hive连接状态: " + (hiveClient.getConnection() != null ? "已连接" : "未连接"));
            
            if (hiveClient.getConnection() == null) {
                System.out.println("Hive未连接，请检查配置");
                return;
            }
            
            System.out.println("\nHive操作菜单:");
            System.out.println("1. 列出所有数据库");
            System.out.println("2. 列出当前数据库中的表");
            System.out.println("3. 执行SQL查询");
            System.out.println("0. 返回主菜单");
            
            System.out.print("\n请选择操作: ");
            String hiveChoice = scanner.nextLine();
            
            switch (hiveChoice) {
                case "1":
                    System.out.println("数据库列表:");
                    hiveClient.getDatabases().forEach(db -> System.out.println("- " + db));
                    break;
                    
                case "2":
                    System.out.println("当前数据库中的表:");
                    hiveClient.getTables().forEach(table -> System.out.println("- " + table));
                    break;
                    
                case "3":
                    System.out.print("请输入SQL查询语句: ");
                    String sql = scanner.nextLine();
                    System.out.println("查询结果:");
                    hiveClient.executeQuery(sql).forEach(row -> {
                        System.out.println("--------------------");
                        row.forEach((key, value) -> System.out.printf("%-20s: %s%n", key, value));
                    });
                    System.out.println("--------------------");
                    break;
                    
                case "0":
                    // 返回主菜单
                    break;
                    
                default:
                    System.out.println("暂未实现该功能，请选择其他操作");
            }
        } catch (Exception e) {
            System.out.println("Hive操作失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 处理Kafka操作
     */
    private static void handleKafkaOperations(ConfigurableApplicationContext context, Scanner scanner) {
        try {
            KafkaClient kafkaClient = context.getBean(KafkaClient.class);
            
            System.out.println("\nKafka操作菜单:");
            System.out.println("1. 列出所有主题");
            System.out.println("2. 创建主题");
            System.out.println("3. 发送消息");
            System.out.println("4. 消费消息");
            System.out.println("5. 删除主题");
            System.out.println("0. 返回主菜单");
            
            System.out.print("\n请选择操作: ");
            String kafkaChoice = scanner.nextLine();
            
            switch (kafkaChoice) {
                case "1":
                    System.out.println("主题列表:");
                    kafkaClient.listTopics().forEach(topic -> System.out.println("- " + topic));
                    break;
                    
                case "2":
                    System.out.print("请输入主题名称: ");
                    String topicName = scanner.nextLine();
                    System.out.print("请输入分区数量 (默认1): ");
                    String partitionsStr = scanner.nextLine();
                    int partitions = partitionsStr.isEmpty() ? 1 : Integer.parseInt(partitionsStr);
                    System.out.print("请输入副本因子 (默认1): ");
                    String replicationStr = scanner.nextLine();
                    short replication = replicationStr.isEmpty() ? 1 : Short.parseShort(replicationStr);
                    
                    boolean created = kafkaClient.createTopic(topicName, partitions, replication);
                    System.out.println(created ? "主题创建成功" : "主题创建失败");
                    break;
                    
                case "3":
                    System.out.print("请输入主题名称: ");
                    String sendTopicName = scanner.nextLine();
                    System.out.print("请输入消息键 (可选): ");
                    String key = scanner.nextLine();
                    key = key.isEmpty() ? null : key;
                    System.out.print("请输入消息内容: ");
                    String value = scanner.nextLine();
                    
                    Map<String, Object> sendResult = kafkaClient.sendMessage(sendTopicName, key, value);
                    System.out.println("消息发送成功: " + sendResult);
                    break;
                    
                case "4":
                    System.out.print("请输入主题名称: ");
                    String consumeTopicName = scanner.nextLine();
                    System.out.print("请输入消费者组ID (可选): ");
                    String groupId = scanner.nextLine();
                    groupId = groupId.isEmpty() ? "console-consumer-" + System.currentTimeMillis() : groupId;
                    System.out.print("请输入最大获取消息数: ");
                    String maxStr = scanner.nextLine();
                    int maxRecords = maxStr.isEmpty() ? 10 : Integer.parseInt(maxStr);
                    
                    System.out.println("正在消费消息，请稍候...");
                    List<Map<String, Object>> messages = kafkaClient.consumeMessages(
                            consumeTopicName, groupId, maxRecords, 5000);
                    
                    System.out.println("消息列表 (" + messages.size() + " 条):");
                    messages.forEach(msg -> {
                        System.out.println("--------------------");
                        System.out.println("偏移量: " + msg.get("offset"));
                        System.out.println("键: " + msg.get("key"));
                        System.out.println("值: " + msg.get("value"));
                        System.out.println("时间戳: " + msg.get("timestamp"));
                    });
                    if (messages.isEmpty()) {
                        System.out.println("未消费到消息");
                    }
                    break;
                    
                case "5":
                    System.out.print("请输入要删除的主题名称: ");
                    String deleteTopicName = scanner.nextLine();
                    boolean deleted = kafkaClient.deleteTopic(deleteTopicName);
                    System.out.println(deleted ? "主题删除成功" : "主题删除失败");
                    break;
                    
                case "0":
                    // 返回主菜单
                    break;
                    
                default:
                    System.out.println("暂未实现该功能，请选择其他操作");
            }
        } catch (Exception e) {
            System.out.println("Kafka操作失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 处理Spark操作
     */
    private static void handleSparkOperations(ConfigurableApplicationContext context, Scanner scanner) {
        try {
            SparkClient sparkClient = context.getBean(SparkClient.class);
            
            System.out.println("\nSpark操作菜单:");
            System.out.println("1. 执行单词统计");
            System.out.println("2. 加载CSV数据并查询");
            System.out.println("3. 加载数据集并查看统计");
            System.out.println("4. 转换文件格式");
            System.out.println("0. 返回主菜单");
            
            System.out.print("\n请选择操作: ");
            String sparkChoice = scanner.nextLine();
            
            switch (sparkChoice) {
                case "1":
                    System.out.print("请输入文本文件路径: ");
                    String filePath = scanner.nextLine();
                    
                    System.out.println("正在执行单词统计...");
                    Map<String, Long> wordCounts = sparkClient.wordCount(filePath);
                    
                    System.out.println("单词统计结果 (前20个):");
                    wordCounts.entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .limit(20)
                            .forEach(entry -> System.out.printf("%-20s: %d%n", entry.getKey(), entry.getValue()));
                    break;
                    
                case "2":
                    System.out.print("请输入CSV文件路径: ");
                    String csvPath = scanner.nextLine();
                    
                    System.out.println("正在加载CSV数据...");
                    org.apache.spark.sql.Dataset<org.apache.spark.sql.Row> dataset = 
                            sparkClient.readCsv(csvPath, true, true);
                    
                    System.out.println("数据集结构:");
                    dataset.printSchema();
                    
                    System.out.println("数据集预览 (前5行):");
                    dataset.show(5, false);
                    
                    System.out.print("请输入SQL查询 (例如: SELECT * FROM dataset LIMIT 10): ");
                    String sqlQuery = scanner.nextLine();
                    
                    if (!sqlQuery.isEmpty()) {
                        dataset.createOrReplaceTempView("dataset");
                        org.apache.spark.sql.Dataset<org.apache.spark.sql.Row> result = 
                                sparkClient.getSparkSession().sql(sqlQuery);
                        
                        System.out.println("查询结果:");
                        result.show(20, false);
                    }
                    break;
                    
                case "3":
                    System.out.print("请输入数据文件路径: ");
                    String datasetPath = scanner.nextLine();
                    System.out.print("请输入文件格式 (csv, json, parquet): ");
                    String format = scanner.nextLine();
                    
                    System.out.println("正在加载数据集...");
                    org.apache.spark.sql.Dataset<org.apache.spark.sql.Row> ds = null;
                    
                    switch (format.toLowerCase()) {
                        case "csv":
                            ds = sparkClient.readCsv(datasetPath, true, true);
                            break;
                        case "json":
                            ds = sparkClient.readJson(datasetPath);
                            break;
                        case "parquet":
                            ds = sparkClient.readParquet(datasetPath);
                            break;
                        default:
                            System.out.println("不支持的文件格式: " + format);
                            return;
                    }
                    
                    System.out.println("数据集统计信息:");
                    org.apache.spark.sql.Dataset<org.apache.spark.sql.Row> stats = 
                            sparkClient.getStatistics(ds);
                    stats.show(false);
                    break;
                    
                case "4":
                    System.out.print("请输入源数据文件路径: ");
                    String srcPath = scanner.nextLine();
                    System.out.print("请输入源文件格式 (csv, json, parquet): ");
                    String srcFormat = scanner.nextLine();
                    System.out.print("请输入目标文件路径: ");
                    String dstPath = scanner.nextLine();
                    System.out.print("请输入目标文件格式 (csv, json, parquet): ");
                    String dstFormat = scanner.nextLine();
                    
                    System.out.println("正在转换文件格式...");
                    org.apache.spark.sql.Dataset<org.apache.spark.sql.Row> srcDs = null;
                    
                    switch (srcFormat.toLowerCase()) {
                        case "csv":
                            srcDs = sparkClient.readCsv(srcPath, true, true);
                            break;
                        case "json":
                            srcDs = sparkClient.readJson(srcPath);
                            break;
                        case "parquet":
                            srcDs = sparkClient.readParquet(srcPath);
                            break;
                        default:
                            System.out.println("不支持的源文件格式: " + srcFormat);
                            return;
                    }
                    
                    switch (dstFormat.toLowerCase()) {
                        case "csv":
                            sparkClient.saveCsv(srcDs, dstPath, true);
                            break;
                        case "json":
                            sparkClient.saveJson(srcDs, dstPath);
                            break;
                        case "parquet":
                            sparkClient.saveParquet(srcDs, dstPath);
                            break;
                        default:
                            System.out.println("不支持的目标文件格式: " + dstFormat);
                            return;
                    }
                    
                    System.out.println("文件格式转换完成，已保存到: " + dstPath);
                    break;
                    
                case "0":
                    // 返回主菜单
                    break;
                    
                default:
                    System.out.println("暂未实现该功能，请选择其他操作");
            }
        } catch (Exception e) {
            System.out.println("Spark操作失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 跨域过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许跨域的头部信息
        config.addAllowedHeader("*");
        // 允许跨域的方法
        config.addAllowedMethod("*");
        // 允许跨域的源
        config.addAllowedOrigin("*");
        // 是否允许携带cookie
        config.setAllowCredentials(true);
        // 跨域允许时间
        config.setMaxAge(3600L);
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
} 