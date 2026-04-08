package com.quant.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 信号提醒实体类
 *
 * 用于存储系统生成的交易信号和提醒记录，包括信号类型、
 * 触发条件、提醒状态、发送渠道等信息。
 *
 * 支持买卖信号、风险预警等多种提醒类型
 */
@Data
@Entity
@Table(name = "signal_alert",
       indexes = {
           @Index(name = "idx_signal_code_time", columnList = "stockCode,signalTime"),
           @Index(name = "idx_signal_status", columnList = "alertStatus"),
           @Index(name = "idx_signal_type", columnList = "signalType")
       })
public class SignalAlert {

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
     * 股票名称
     */
    @Column(name = "stock_name", length = 50)
    private String stockName;

    /**
     * 信号类型
     * BUY: 买入信号
     * SELL: 卖出信号
     * RISK_WARNING: 风险预警
     * PRICE_BREAK: 价格突破
     * INDICATOR_CROSS: 指标交叉
     * STOP_LOSS: 止损提醒
     * STOP_PROFIT: 止盈提醒
     */
    @Column(name = "signal_type", nullable = false, length = 30)
    private String signalType;

    /**
     * 信号触发时间
     */
    @Column(name = "signal_time", nullable = false)
    private LocalDateTime signalTime;

    /**
     * 信号触发价格
     */
    @Column(name = "trigger_price", precision = 10, scale = 3)
    private BigDecimal triggerPrice;

    /**
     * 信号触发条件描述
     */
    @Column(name = "trigger_condition", length = 500)
    private String triggerCondition;

    /**
     * 信号强度
     * STRONG: 强信号
     * MEDIUM: 中等信号
     * WEAK: 弱信号
     */
    @Column(name = "signal_strength", length = 20)
    private String signalStrength;

    /**
     * 建议操作
     * BUY: 建议买入
     * SELL: 建议卖出
     * HOLD: 建议持有
     * WATCH: 建议观察
     */
    @Column(name = "suggestion", length = 20)
    private String suggestion;

    /**
     * 提醒状态
     * PENDING: 待发送
     * SENT: 已发送
     * FAILED: 发送失败
     * IGNORED: 已忽略
     * HANDLED: 已处理
     */
    @Column(name = "alert_status", length = 20)
    private String alertStatus;

    /**
     * 发送渠道，逗号分隔
     */
    @Column(name = "send_channels", length = 200)
    private String sendChannels;

    /**
     * 发送时间
     */
    @Column(name = "send_time")
    private LocalDateTime sendTime;

    /**
     * 发送失败原因
     */
    @Column(name = "fail_reason", length = 500)
    private String failReason;

    /**
     * 用户备注
     */
    @Column(name = "user_note", length = 500)
    private String userNote;

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
}