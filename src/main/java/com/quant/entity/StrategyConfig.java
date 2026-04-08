package com.quant.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 策略配置实体类
 *
 * 用于存储用户的策略配置信息，包括策略类型、参数设置、
 * 触发条件、目标股票池等配置。
 *
 * 支持多种策略类型：均线策略、MACD策略、KDJ策略、布林带策略等
 */
@Data
@Entity
@Table(name = "strategy_config",
       indexes = {
           @Index(name = "idx_strategy_user", columnList = "userId"),
           @Index(name = "idx_strategy_status", columnList = "status")
       })
public class StrategyConfig {

    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID（预留多用户支持）
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 策略名称
     */
    @Column(name = "strategy_name", nullable = false, length = 100)
    private String strategyName;

    /**
     * 策略类型
     * MA_GOLDEN_CROSS: 均线金叉策略
     * MACD_CROSS: MACD交叉策略
     * KDJ_OVERSOLD: KDJ超卖策略
     * BOLL_BAND: 布林带策略
     * COMBO: 组合策略
     * CUSTOM: 自定义策略
     */
    @Column(name = "strategy_type", nullable = false, length = 30)
    private String strategyType;

    /**
     * 目标股票代码列表，逗号分隔
     * 如: 600519,000001,000002
     */
    @Column(name = "target_stocks", length = 500)
    private String targetStocks;

    /**
     * 策略参数JSON
     * 存储各策略的特定参数，如均线周期、阈值等
     * 示例: {"maShort":5,"maLong":20,"crossType":"golden"}
     */
    @Column(name = "strategy_params", columnDefinition = "TEXT")
    private String strategyParams;

    /**
     * 买入条件表达式
     * 支持技术指标组合条件
     */
    @Column(name = "buy_condition", columnDefinition = "TEXT")
    private String buyCondition;

    /**
     * 卖出条件表达式
     */
    @Column(name = "sell_condition", columnDefinition = "TEXT")
    private String sellCondition;

    /**
     * 止损比例（百分比）
     */
    @Column(name = "stop_loss_pct", precision = 6, scale = 2)
    private Double stopLossPct;

    /**
     * 止盈比例（百分比）
     */
    @Column(name = "stop_profit_pct", precision = 6, scale = 2)
    private Double stopProfitPct;

    /**
     * 策略状态
     * ACTIVE: 活跃运行中
     * PAUSED: 已暂停
     * DISABLED: 已禁用
     * DELETED: 已删除
     */
    @Column(name = "status", length = 20)
    private String status;

    /**
     * 是否启用提醒
     */
    @Column(name = "alert_enabled")
    private Boolean alertEnabled;

    /**
     * 提醒渠道，逗号分隔
     * EMAIL, DINGTALK, FEISHU, WECHAT_WORK
     */
    @Column(name = "alert_channels", length = 200)
    private String alertChannels;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 备注
     */
    @Column(name = "remark", length = 500)
    private String remark;
}