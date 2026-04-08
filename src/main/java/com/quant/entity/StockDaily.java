package com.quant.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日线数据实体类
 *
 * <p>对应数据库表：stock_daily</p>
 *
 * <p>存储股票的日线行情数据，每个交易日一条记录</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>stockCode: 股票代码</li>
 *   <li>tradeDate: 交易日期</li>
 *   <li>open: 开盘价</li>
 *   <li>close: 收盘价</li>
 *   <li>high: 最高价</li>
 *   <li>low: 最低价</li>
 *   <li>volume: 成交量（手）</li>
 *   <li>amount: 成交额（元）</li>
 *   <li>changePct: 涨跌幅（%）</li>
 *   <li>turnover: 换手率（%）</li>
 * </ul>
 *
 * @author quant-system
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stock_daily", indexes = {
    @Index(name = "idx_trade_date", columnList = "trade_date"),
    @Index(name = "idx_stock_code", columnList = "stock_code")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_code_date", columnNames = {"stock_code", "trade_date"})
})
public class StockDaily {

    /** 主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 股票代码，如：000001 */
    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    /** 交易日期 */
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    /** 开盘价 */
    @Column(name = "open", precision = 10, scale = 3)
    private BigDecimal open;

    /** 收盘价 */
    @Column(name = "close", precision = 10, scale = 3)
    private BigDecimal close;

    /** 最高价 */
    @Column(name = "high", precision = 10, scale = 3)
    private BigDecimal high;

    /** 最低价 */
    @Column(name = "low", precision = 10, scale = 3)
    private BigDecimal low;

    /** 成交量（手） */
    @Column(name = "volume")
    private Long volume;

    /** 成交额（元） */
    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    /** 涨跌幅（%） */
    @Column(name = "change_pct", precision = 8, scale = 4)
    private BigDecimal changePct;

    /** 换手率（%） */
    @Column(name = "turnover", precision = 8, scale = 4)
    private BigDecimal turnover;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * JPA 生命周期回调 - 持久化前执行
     * 自动设置创建时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}