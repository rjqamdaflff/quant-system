package com.quant.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * 数据采集日志实体类
 *
 * <p>对应数据库表：collect_log</p>
 *
 * <p>记录数据采集过程中的状态信息，包括：</p>
 * <ul>
 *   <li>采集类型（daily/minute/financial等）</li>
 *   <li>采集状态（success/failed/partial）</li>
 *   <li>采集记录数</li>
 *   <li>错误信息（失败时）</li>
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
@Table(name = "collect_log", indexes = {
    @Index(name = "idx_collect_type", columnList = "collect_type"),
    @Index(name = "idx_status", columnList = "status")
})
public class CollectLog {

    /** 主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 采集类型
     * <ul>
     *   <li>stock_list: 股票列表采集</li>
     *   <li>daily: 日线数据采集</li>
     *   <li>minute: 分钟线数据采集</li>
     *   <li>financial: 财务数据采集</li>
     * </ul>
     */
    @Column(name = "collect_type", nullable = false, length = 20)
    private String collectType;

    /** 股票代码（可选，列表采集时为null） */
    @Column(name = "stock_code", length = 10)
    private String stockCode;

    /** 开始时间 */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /** 结束时间 */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * 采集状态
     * <ul>
     *   <li>success: 成功</li>
     *   <li>failed: 失败</li>
     *   <li>partial: 部分成功</li>
     * </ul>
     */
    @Column(name = "status", nullable = false, length = 10)
    private String status;

    /** 采集记录数 */
    @Column(name = "record_count")
    private Integer recordCount;

    /** 错误信息（失败时记录） */
    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * JPA 生命周期回调 - 持久化前执行
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}