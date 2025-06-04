package org.shiguang.ws.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 股票数据模型类
 * 用于表示从东方财富网站获取的股票K线数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 股票代码
     */
    private String code;
    
    /**
     * 股票市场 (0: 深圳, 1: 上海)
     */
    private Integer market;
    
    /**
     * 股票名称
     */
    private String name;
    
    /**
     * 交易日期 (格式：yyyy-MM-dd)
     */
    private String tradeDate;
    
    /**
     * 开盘价
     */
    private Double openPrice;
    
    /**
     * 收盘价
     */
    private Double closePrice;
    
    /**
     * 最高价
     */
    private Double highPrice;
    
    /**
     * 最低价
     */
    private Double lowPrice;
    
    /**
     * 成交量
     */
    private Long volume;
    
    /**
     * 成交额
     */
    private Double amount;
    
    /**
     * 振幅 (%)
     */
    private Double amplitude;
    
    /**
     * 涨跌幅 (%)
     */
    private Double changePercent;
    
    /**
     * 涨跌额
     */
    private Double change;
    
    /**
     * 换手率 (%)
     */
    private Double turnoverRate;
} 