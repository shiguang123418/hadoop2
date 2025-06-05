package org.shiguang;

import org.shiguang.utils.AppConstants;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.concurrent.Executor;

/**
 * Hadoop服务应用程序入口
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@ServletComponentScan
public class HadoopApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(HadoopApplication.class, args);


        String serverPort = context.getEnvironment().getProperty("server.port", "8080");
        String apiPath = context.getEnvironment().getProperty("server.servlet.context-path", "/api");
        String serverHost = context.getEnvironment().getProperty("app.server.host", "localhost");
        printApplicationBanner(serverHost, serverPort, apiPath);

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

} 