package org.shiguang.module.stock.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 股票配置管理
 * 用于集中管理股票数据爬取的各项配置
 */
@Component
public class StockConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(StockConfig.class);
    
    @Value("${stock.enabled:true}")
    private boolean enabled;
    
    @Value("${stock.crawler.interval:10000}")
    private int crawlerInterval;
    
    @Value("${stock.crawler.default-code:000001}")
    private String defaultStockCode;
    
    @Value("${stock.processor.batch-size:100}")
    private int processorBatchSize;
    
    @Value("${stock.analysis.volatility-threshold:5.0}")
    private double volatilityThreshold;

    @Value("${stock.simulation.enabled:false}")
    private boolean simulationEnabled;

    @Value("${stock.simulation.api.host:http://localhost:8080}")
    private String simulationApiHost;
    
    @Value("${stock.simulation.target-code:002714}")
    private String simulationTargetCode;
    
    @Value("${stock.simulation.target-name:牧原股份}")
    private String simulationTargetName;

    /**
     * 检查股票模块是否全局启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 设置股票模块是否全局启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        logger.info("股票模块已{}", enabled ? "启用" : "禁用");
    }

    public int getCrawlerInterval() {
        return crawlerInterval;
    }

    public void setCrawlerInterval(int crawlerInterval) {
        if (crawlerInterval < 1000) {
            logger.warn("爬取间隔设置过小 ({}ms)，已调整为最小值1000ms", crawlerInterval);
            this.crawlerInterval = 1000;
        } else {
            this.crawlerInterval = crawlerInterval;
            logger.info("股票爬取间隔已设置为{}毫秒", crawlerInterval);
        }
    }

    public String getDefaultStockCode() {
        return defaultStockCode;
    }

    public void setDefaultStockCode(String defaultStockCode) {
        this.defaultStockCode = defaultStockCode;
        logger.info("默认股票代码已设置为{}", defaultStockCode);
    }

    public int getProcessorBatchSize() {
        return processorBatchSize;
    }

    public void setProcessorBatchSize(int processorBatchSize) {
        this.processorBatchSize = processorBatchSize;
        logger.info("处理器批处理大小已设置为{}", processorBatchSize);
    }

    public double getVolatilityThreshold() {
        return volatilityThreshold;
    }

    public void setVolatilityThreshold(double volatilityThreshold) {
        this.volatilityThreshold = volatilityThreshold;
        logger.info("波动率阈值已设置为{}%", volatilityThreshold);
    }
    
    public boolean isSimulationEnabled() {
        return simulationEnabled;
    }
    
    public void setSimulationEnabled(boolean simulationEnabled) {
        this.simulationEnabled = simulationEnabled;
        logger.info("股票数据模拟已{}", simulationEnabled ? "启用" : "禁用");
    }
    
    public String getSimulationApiHost() {
        return simulationApiHost;
    }
    
    public void setSimulationApiHost(String simulationApiHost) {
        this.simulationApiHost = simulationApiHost;
        logger.info("模拟API主机地址已设置为{}", simulationApiHost);
    }
    
    public String getSimulationTargetCode() {
        return simulationTargetCode;
    }
    
    public void setSimulationTargetCode(String simulationTargetCode) {
        this.simulationTargetCode = simulationTargetCode;
        logger.info("模拟目标股票代码已设置为{}", simulationTargetCode);
    }
    
    public String getSimulationTargetName() {
        return simulationTargetName;
    }
    
    public void setSimulationTargetName(String simulationTargetName) {
        this.simulationTargetName = simulationTargetName;
        logger.info("模拟目标股票名称已设置为{}", simulationTargetName);
    }
} 