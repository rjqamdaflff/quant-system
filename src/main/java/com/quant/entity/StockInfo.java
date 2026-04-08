package com.quant.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票基本信息实体类
 *
 * <p>对应数据库表：stock_info</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>stockCode: 股票代码（如：000001）</li>
 *   <li>stockName: 股票名称（如：平安银行）</li>
 *   <li>industry: 所属行业（如：银行）</li>
 *   <li>market: 所属市场（sh=沪市, sz=深市）</li>
 *   <li>listDate: 上市日期</li>
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
@Table(name = "stock_info", indexes = {
    @Index(name = "idx_stock_code", columnList = "stock_code")
})
public class StockInfo {

    /** 主键ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 股票代码，如：000001 */
    @Column(name = "stock_code", nullable = false, unique = true, length = 10)
    private String stockCode;

    /** 股票名称，如：平安银行 */
    @Column(name = "stock_name", nullable = false, length = 50)
    private String stockName;

    /** 所属行业 */
    @Column(name = "industry", length = 50)
    private String industry;

    /** 所属市场（sh=沪市, sz=深市） */
    @Column(name = "market", length = 10)
    private String market;

    /** 上市日期 */
    @Column(name = "list_date")
    private LocalDate listDate;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * JPA 生命周期回调 - 持久化前执行
     * 自动设置创建时间和更新时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * JPA 生命周期回调 - 更新前执行
     * 自动更新更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}