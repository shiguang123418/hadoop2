package org.shiguang;

import org.shiguang.hadoop.HDFSClient;
import org.shiguang.hadoop.HiveClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.util.Scanner;

/**
 * HDFS和Hive模块启动类
 */
@SpringBootApplication
public class HadoopApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(HadoopApplication.class, args);
        
        System.out.println("====== HDFS和Hive接口模块已启动 ======");
        System.out.println("REST API接口：");
        System.out.println("- HDFS API: http://192.168.1.192:8080/api/hdfs");
        System.out.println("- Hive API: http://192.168.1.192:8080/api/hive");
        System.out.println("\n可用控制台服务：");
        System.out.println("1. HDFS文件操作");
        System.out.println("2. Hive查询");
        System.out.println("0. 退出");
        
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        while (running) {
            System.out.print("\n请选择要使用的服务 (0-2): ");
            String choice = scanner.nextLine();
            
            try {
                switch (choice) {
                    case "1":
                        // HDFS操作示例
                        HDFSClient hdfsClient = context.getBean(HDFSClient.class);
                        System.out.println("HDFS连接状态: " + (hdfsClient.getFileSystem() != null ? "已连接" : "未连接"));
                        System.out.println("HDFS URI: " + hdfsClient.getFileSystem().getUri());
                        
                        System.out.println("\nHDFS操作菜单:");
                        System.out.println("1. 列出目录内容");
                        System.out.println("2. 创建目录");
                        System.out.println("3. 上传文件");
                        System.out.println("4. 下载文件");
                        System.out.println("5. 删除文件/目录");
                        System.out.println("0. 返回主菜单");
                        
                        System.out.print("\n请选择操作: ");
                        String hdfsChoice = scanner.nextLine();
                        
                        switch (hdfsChoice) {
                            case "1":
                                System.out.print("请输入要列出内容的目录路径: ");
                                String listPath = scanner.nextLine();
                                try {
                                    hdfsClient.listFiles(listPath).forEach(file -> {
                                        System.out.printf("%-30s %-10s %-10s %s%n",
                                                file.get("name"),
                                                Boolean.TRUE.equals(file.get("isDirectory")) ? "<DIR>" : file.get("length") + " bytes",
                                                file.get("owner"),
                                                file.get("permission"));
                                    });
                                } catch (Exception e) {
                                    System.out.println("列出目录失败: " + e.getMessage());
                                }
                                break;
                                
                            case "2":
                                System.out.print("请输入要创建的目录路径: ");
                                String dirPath = scanner.nextLine();
                                try {
                                    boolean success = hdfsClient.mkdir(dirPath);
                                    System.out.println(success ? "目录创建成功" : "目录已存在");
                                } catch (Exception e) {
                                    System.out.println("创建目录失败: " + e.getMessage());
                                }
                                break;
                            
                            case "0":
                                // 返回主菜单
                                break;
                                
                            default:
                                System.out.println("暂未实现该功能，请选择其他操作");
                        }
                        break;
                        
                    case "2":
                        // Hive查询示例
                        HiveClient hiveClient = context.getBean(HiveClient.class);
                        System.out.println("Hive连接状态: " + (hiveClient.getConnection() != null ? "已连接" : "未连接"));
                        
                        System.out.println("\nHive操作菜单:");
                        System.out.println("1. 列出所有数据库");
                        System.out.println("2. 列出当前数据库中的表");
                        System.out.println("3. 执行SQL查询");
                        System.out.println("0. 返回主菜单");
                        
                        System.out.print("\n请选择操作: ");
                        String hiveChoice = scanner.nextLine();
                        
                        switch (hiveChoice) {
                            case "1":
                                try {
                                    System.out.println("数据库列表:");
                                    hiveClient.getDatabases().forEach(db -> System.out.println("- " + db));
                                } catch (Exception e) {
                                    System.out.println("获取数据库列表失败: " + e.getMessage());
                                }
                                break;
                                
                            case "2":
                                try {
                                    System.out.println("当前数据库中的表:");
                                    hiveClient.getTables().forEach(table -> System.out.println("- " + table));
                                } catch (Exception e) {
                                    System.out.println("获取表列表失败: " + e.getMessage());
                                }
                                break;
                                
                            case "3":
                                System.out.print("请输入SQL查询语句: ");
                                String sql = scanner.nextLine();
                                try {
                                    System.out.println("查询结果:");
                                    hiveClient.executeQuery(sql).forEach(row -> {
                                        System.out.println("--------------------");
                                        row.forEach((key, value) -> System.out.printf("%-20s: %s%n", key, value));
                                    });
                                    System.out.println("--------------------");
                                } catch (Exception e) {
                                    System.out.println("执行查询失败: " + e.getMessage());
                                }
                                break;
                                
                            case "0":
                                // 返回主菜单
                                break;
                                
                            default:
                                System.out.println("暂未实现该功能，请选择其他操作");
                        }
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
} 