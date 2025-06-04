package org.shiguang.module.stock.Controller;

import org.shiguang.module.stock.config.StockConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 股票模拟API控制器
 * 模拟东方财富API，在真实API不可用时提供模拟数据
 */
@RestController
@RequestMapping("/api/stock-simulation")
public class StockSimulationController {

    private static final Logger logger = LoggerFactory.getLogger(StockSimulationController.class);
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // 保存每只股票的最后价格，以便生成连续的价格数据
    private final ConcurrentHashMap<String, Double> lastPriceMap = new ConcurrentHashMap<>();
    
    @Autowired
    private StockConfig stockConfig;
    
    /**
     * 模拟东方财富API接口
     * 直接返回JSONP格式的模拟股票数据
     */
    @GetMapping(value = "/kline", produces = "application/javascript;charset=UTF-8")
    public String getSimulatedKlineData(
            @RequestParam(value = "cb", required = false, defaultValue = "jQuery") String callback,
            @RequestParam(value = "secid", required = false, defaultValue = "0.002714") String secid) {
        
        logger.info("请求模拟K线数据: callback={}, secid={}", callback, secid);
        
        // 解析股票代码和市场
        String[] parts = secid.split("\\.");
        int market = 0;
        String code = "002714"; // 默认牧原股份
        String name = "牧原股份";
        
        if (parts.length == 2) {
            try {
                market = Integer.parseInt(parts[0]);
                code = parts[1];
            } catch (NumberFormatException e) {
                logger.warn("解析secid失败: {}", secid);
            }
        }
        
        // 生成模拟数据
        String simulatedData = generateSimulatedKlineData(code, name, market);
        
        // 构建完整的JSONP响应
        String response = String.format("%s(%s)", callback, simulatedData);
        
        logger.debug("返回模拟数据: {}", response);
        return response;
    }
    
    /**
     * 生成模拟的K线数据JSON
     */
    private String generateSimulatedKlineData(String code, String name, int market) {
        // 获取或初始化股票的基础价格
        Double lastPrice = lastPriceMap.getOrDefault(code, 41.54); // 默认价格为41.54
        
        // 生成随机价格波动（上下3%）
        double priceFluctuation = lastPrice * (random.nextDouble() * 0.06 - 0.03);
        double openPrice = roundToTwoDecimals(lastPrice + (random.nextDouble() * 0.02 - 0.01) * lastPrice);
        double closePrice = roundToTwoDecimals(lastPrice + priceFluctuation);
        double highPrice = roundToTwoDecimals(Math.max(openPrice, closePrice) * (1 + random.nextDouble() * 0.01));
        double lowPrice = roundToTwoDecimals(Math.min(openPrice, closePrice) * (1 - random.nextDouble() * 0.01));
        
        // 确保最高价和最低价的正确性
        highPrice = Math.max(highPrice, Math.max(openPrice, closePrice));
        lowPrice = Math.min(lowPrice, Math.min(openPrice, closePrice));
        
        // 计算成交量和成交额
        long volume = 400000 + random.nextInt(200000); // 40-60万股
        double amount = roundToTwoDecimals(volume * closePrice / 10000); // 单位调整为万元
        
        // 计算振幅、涨跌幅等数据
        double amplitude = roundToTwoDecimals((highPrice - lowPrice) / lastPrice * 100); // 振幅
        double changePercent = roundToTwoDecimals((closePrice - lastPrice) / lastPrice * 100); // 涨跌幅
        double change = roundToTwoDecimals(closePrice - lastPrice); // 涨跌额
        double turnoverRate = roundToTwoDecimals(random.nextDouble() * 2); // 换手率
        
        // 更新最后价格
        lastPriceMap.put(code, closePrice);
        
        // 获取今天日期
        String today = LocalDate.now().format(DATE_FORMATTER);
        
        // 构建K线数据字符串
        String klineData = String.format("%s,%.2f,%.2f,%.2f,%.2f,%d,%.2f,%.2f,%.2f,%.2f,%.2f",
                today, openPrice, closePrice, highPrice, lowPrice, volume, amount, 
                amplitude, changePercent, change, turnoverRate);
        
        // 构建完整的JSON响应
        return String.format(
                "{\"rc\":0,\"rt\":17,\"svr\":%d,\"lt\":1,\"full\":0,\"dlmkts\":\"\",\"data\":{" +
                "\"code\":\"%s\",\"market\":%d,\"name\":\"%s\",\"decimal\":2,\"dktotal\":2732," +
                "\"preKPrice\":%.2f,\"klines\":[\"%s\"]}}",
                random.nextInt(900000) + 100000, // 随机服务器ID
                code, market, name, lastPrice, klineData);
    }
    
    /**
     * 将数字四舍五入到两位小数
     */
    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100) / 100.0;
    }
    
    /**
     * 重置模拟数据
     */
    @PostMapping("/reset")
    public String resetSimulationData() {
        lastPriceMap.clear();
        logger.info("模拟数据已重置");
        return "{\"success\":true,\"message\":\"模拟数据已重置\"}";
    }
    
    /**
     * 设置特定股票的基础价格
     */
    @PostMapping("/set-price")
    public String setBasePrice(
            @RequestParam("code") String code,
            @RequestParam("price") double price) {
        lastPriceMap.put(code, price);
        logger.info("已设置股票{}的基础价格为{}", code, price);
        return String.format("{\"success\":true,\"message\":\"已设置股票%s的基础价格为%.2f\"}", code, price);
    }
} 