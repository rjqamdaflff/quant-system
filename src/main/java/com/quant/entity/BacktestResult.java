package com.quant.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 回测结果实体类
 *
 * 用于存储策略回测的执行结果，包括回测参数、收益指标、
 * 交易记录、风险指标等关键数据。
 *
 * 支持多策略对比分析，保存历史回测记录
 */
@Data
@Entity
@Table(name = "backtest_result",
       indexes = {
           @Index(name = "idx_backtest_strategy", columnList = "strategyId"),
           @Index(name = "idx_backtest_code", columnList = "stockCode"),
           @Index(name = "idx_backtest_time", columnList = "backtestTime")
       })
public class BacktestResult {

    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的策略配置ID
     */
    @Column(name = "strategy_id")
    private Long strategyId;

    /**
     * 股票代码
     */
    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    /**
     * 回测开始日期
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * 回测结束日期
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * 初始资金（元）
     */
    @Column(name = "initial_capital", precision = 18, scale = 2)
    private BigDecimal initialCapital;

    /**
     * 最终资金（元）
     */
    @Column(name = "final_capital", precision = 18, scale = 2)
    private BigDecimal finalCapital;

    /**
     * 总收益率（百分比）
     */
    @Column(name = "total_return", precision = 10, scale = 4)
    private BigDecimal totalReturn;

    /**
     * 年化收益率（百分比）
     */
    @Column(name = "annual_return", precision = 10, scale = 4)
    private BigDecimal annualReturn;

    /**
     * 最大回撤（百分比）
     */
    @Column(name = "max_drawdown", precision = 10, scale = 4)
    private BigDecimal maxDrawdown;

    /**
     * 夏普比率
     */
    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    private BigDecimal sharpeRatio;

    /**
     * 总交易次数
     */
    @Column(name = "total_trades")
    private Integer totalTrades;

    /**
     * 盈利交易次数
     */
    @Column(name = "win_trades")
    private Integer winTrades;

    /**
     * 亏损交易次数
     */
    @Column(name = "loss_trades")
    private Integer lossTrades;

    /**
     *胜率（百分比）
     */
    @Column(name = "win_rate", precision = 6, scale = 2)
    private BigDecimal winRate;

    /**
     * 平均盈利（元）
     */
    @Column(name = "avg_profit", precision = 12, scale = 2)
    private BigDecimal avgProfit;

    /**
     * 平均亏损（元）
     */
    @Column(name = "avg_loss", precision = 12, scale = 2)
    private BigDecimal avgLoss;

    /**
     * 盈亏比
     */
    @Column(name = "profit_loss_ratio", precision = 10, scale = 4)
    private BigDecimal profitLossRatio;

    /**
     * 交易记录JSON
     * 存储每次交易的详细信息：买入/卖出时间、价格、数量
     */
    @Column(name = "trade_records", columnDefinition = "TEXT")
    private String tradeRecords;

    /**
     * 回测参数JSON
     * 存储回测使用的策略参数
     */
    @Column(name = "backtest_params", columnDefinition = "TEXT")
    private String backtestParams;

    /**
     * 回测执行时间
     */
    @Column(name = "backtest_time")
    private LocalDateTime backtestTime;

    /**
     * 回测状态
     * RUNNING: 执行中
     * COMPLETED: 已完成
     * FAILED: 执行失败
     */
    @Column(name = "backtest_status", length = 20)
    private String backtestStatus;

    /**
     * 错误信息（失败时记录）
     */
    @Column(name = "error_msg", length = 500)
    private String errorMsg;
}