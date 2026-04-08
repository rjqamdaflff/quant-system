package com.quant.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 股票财务数据实体类
 *
 * 用于存储股票的财务指标数据，包括营收、净利润、净资产、
 * 每股收益、市盈率、市净率等关键财务指标。
 *
 * 数据来源：AKShare财务数据接口
 * 更新频率：季度报告、年度报告
 */
@Data
@Entity
@Table(name = "stock_financial",
       indexes = {
           @Index(name = "idx_financial_code_date", columnList = "stockCode,reportDate")
       })
public class StockFinancial {

    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 股票代码
     */
    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    /**
     * 报告日期（财报发布日期）
     */
    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    /**
     * 报告类型
     * Q1: 一季度报告
     * Q2: 半年报
     * Q3: 三季度报告
     * Q4: 年度报告
     */
    @Column(name = "report_type", length = 10)
    private String reportType;

    /**
     * 营业总收入（元）
     */
    @Column(name = "total_revenue", precision = 18, scale = 2)
    private BigDecimal totalRevenue;

    /**
     * 营业成本（元）
     */
    @Column(name = "operating_cost", precision = 18, scale = 2)
    private BigDecimal operatingCost;

    /**
     * 净利润（元）
     */
    @Column(name = "net_profit", precision = 18, scale = 2)
    private BigDecimal netProfit;

    /**
     * 归母净利润（元）
     */
    @Column(name = "net_profit_parent", precision = 18, scale = 2)
    private BigDecimal netProfitParent;

    /**
     * 总资产（元）
     */
    @Column(name = "total_assets", precision = 18, scale = 2)
    private BigDecimal totalAssets;

    /**
     * 总负债（元）
     */
    @Column(name = "total_liabilities", precision = 18, scale = 2)
    private BigDecimal totalLiabilities;

    /**
     * 净资产（元）
     */
    @Column(name = "net_assets", precision = 18, scale = 2)
    private BigDecimal netAssets;

    /**
     * 每股收益（元）
     */
    @Column(name = "eps", precision = 10, scale = 4)
    private BigDecimal eps;

    /**
     * 每股净资产（元）
     */
    @Column(name = "bps", precision = 10, scale = 4)
    private BigDecimal bps;

    /**
     * 每股经营现金流（元）
     */
    @Column(name = "ocf_per_share", precision = 10, scale = 4)
    private BigDecimal ocfPerShare;

    /**
     * 市盈率（PE）
     */
    @Column(name = "pe_ratio", precision = 10, scale = 4)
    private BigDecimal peRatio;

    /**
     * 市净率（PB）
     */
    @Column(name = "pb_ratio", precision = 10, scale = 4)
    private BigDecimal pbRatio;

    /**
     * 净资产收益率（ROE）
     */
    @Column(name = "roe", precision = 8, scale = 4)
    private BigDecimal roe;

    /**
     * 资产负债率
     */
    @Column(name = "debt_ratio", precision = 8, scale = 4)
    private BigDecimal debtRatio;

    /**
     * 毛利率
     */
    @Column(name = "gross_margin", precision = 8, scale = 4)
    private BigDecimal grossMargin;

    /**
     * 净利率
     */
    @Column(name = "net_margin", precision = 8, scale = 4)
    private BigDecimal netMargin;

    /**
     * 数据来源
     */
    @Column(name = "data_source", length = 20)
    private String dataSource;

    /**
     * 数据采集时间
     */
    @Column(name = "collect_time")
    private LocalDate collectTime;
}