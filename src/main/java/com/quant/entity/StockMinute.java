package com.quant.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 股票分钟线数据实体类
 *
 * 用于存储股票的分钟级别行情数据，包括每分钟的开盘价、收盘价、
 * 最高价、最低价、成交量、成交额等信息。
 *
 * 数据来源：AKShare/Baostock Python脚本采集
 * 数据频率：1分钟、5分钟、15分钟、30分钟、60分钟
 */
@Data
@Entity
@Table(name = "stock_minute",
       indexes = {
           @Index(name = "idx_minute_code_time", columnList = "stockCode,minuteTime"),
           @Index(name = "idx_minute_time", columnList = "minuteTime")
       })
public class StockMinute {

    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 股票代码，如 600519、000001
     */
    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    /**
     * 分钟时间戳，精确到分钟
     */
    @Column(name = "minute_time", nullable = false)
    private LocalDateTime minuteTime;

    /**
     * 分钟周期类型
     * 1: 1分钟
     * 5: 5分钟
     * 15: 15分钟
     * 30: 30分钟
     * 60: 60分钟
     */
    @Column(name = "period_type", nullable = false)
    private Integer periodType;

    /**
     * 开盘价
     */
    @Column(name = "open_price", precision = 10, scale = 3)
    private BigDecimal openPrice;

    /**
     * 收盘价
     */
    @Column(name = "close_price", precision = 10, scale = 3)
    private BigDecimal closePrice;

    /**
     * 最高价
     */
    @Column(name = "high_price", precision = 10, scale = 3)
    private BigDecimal highPrice;

    /**
     * 最低价
     */
    @Column(name = "low_price", precision = 10, scale = 3)
    private BigDecimal lowPrice;

    /**
     * 成交量（股数）
     */
    @Column(name = "volume", precision = 18)
    private Long volume;

    /**
     * 成交额（元）
     */
    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    /**
     * 涨跌幅（百分比）
     */
    @Column(name = "pct_chg", precision = 8, scale = 4)
    private BigDecimal pctChg;

    /**
     * 数据来源标识
     * AKSHARE: 来自AKShare数据源
     * BAOSTOCK: 来自Baostock数据源
     */
    @Column(name = "data_source", length = 20)
    private String dataSource;

    /**
     * 数据采集时间
     */
    @Column(name = "collect_time")
    private LocalDateTime collectTime;

    /**
     * 数据状态
     * NORMAL: 正常数据
     * SUSPENDED: 停牌期间
     * ADJUSTED: 已复权
     * ABNORMAL: 异常数据待确认
     */
    @Column(name = "data_status", length = 20)
    private String dataStatus;
}