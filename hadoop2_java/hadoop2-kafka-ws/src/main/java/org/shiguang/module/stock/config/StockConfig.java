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
    
    @Value("${stock.crawler.enabled:false}")
    private boolean crawlerEnabled;
    
    @Value("${stock.crawler.interval:10000}")
    private int crawlerInterval;
    
    @Value("${stock.crawler.default-code:000001}")
    private String defaultStockCode;
    
    @Value("${stock.processor.enabled:true}")
    private boolean processorEnabled;
    
    @Value("${stock.processor.batch-size:100}")
    private int processorBatchSize;
    
    @Value("${stock.analysis.enabled:true}")
    private boolean analysisEnabled;
    
    @Value("${stock.analysis.volatility-threshold:5.0}")
    private double volatilityThreshold;

    public boolean isCrawlerEnabled() {
        return crawlerEnabled;
    }

    public void setCrawlerEnabled(boolean crawlerEnabled) {
        this.crawlerEnabled = crawlerEnabled;
        logger.info("股票爬取已{}", crawlerEnabled ? "启用" : "禁用");
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

    public boolean isProcessorEnabled() {
        return processorEnabled;
    }

    public void setProcessorEnabled(boolean processorEnabled) {
        this.processorEnabled = processorEnabled;
        logger.info("股票处理器已{}", processorEnabled ? "启用" : "禁用");
    }

    public int getProcessorBatchSize() {
        return processorBatchSize;
    }

    public void setProcessorBatchSize(int processorBatchSize) {
        this.processorBatchSize = processorBatchSize;
        logger.info("处理器批处理大小已设置为{}", processorBatchSize);
    }

    public boolean isAnalysisEnabled() {
        return analysisEnabled;
    }

    public void setAnalysisEnabled(boolean analysisEnabled) {
        this.analysisEnabled = analysisEnabled;
        logger.info("股票分析已{}", analysisEnabled ? "启用" : "禁用");
    }

    public double getVolatilityThreshold() {
        return volatilityThreshold;
    }

    public void setVolatilityThreshold(double volatilityThreshold) {
        this.volatilityThreshold = volatilityThreshold;
        logger.info("波动率阈值已设置为{}%", volatilityThreshold);
    }
} 