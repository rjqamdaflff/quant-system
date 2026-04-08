package com.quant.repository;

import com.quant.entity.StockFinancial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 股票财务数据仓库接口
 *
 * 提供财务数据的CRUD操作和自定义查询方法
 */
@Repository
public interface StockFinancialRepository extends JpaRepository<StockFinancial, Long> {

    /**
     * 根据股票代码查询财务数据
     * @param stockCode 股票代码
     * @return 财务数据列表
     */
    List<StockFinancial> findByStockCode(String stockCode);

    /**
     * 根据股票代码和报告日期查询
     * @param stockCode 股票代码
     * @param reportDate 报告日期
     * @return 财务数据
     */
    Optional<StockFinancial> findByStockCodeAndReportDate(String stockCode, LocalDate reportDate);

    /**
     * 查询指定股票最新的财务数据
     * @param stockCode 股票代码
     * @return 最新财务数据
     */
    @Query("SELECT sf FROM StockFinancial sf WHERE sf.stockCode = :stockCode " +
           "ORDER BY sf.reportDate DESC LIMIT 1")
    Optional<StockFinancial> findLatestByStockCode(@Param("stockCode") String stockCode);

    /**
     * 根据股票代码和报告类型查询
     * @param stockCode 股票代码
     * @param reportType 报告类型
     * @return 财务数据列表
     */
    List<StockFinancial> findByStockCodeAndReportTypeOrderByReportDateDesc(
        String stockCode, String reportType
    );

    /**
     * 查询指定时间范围内的财务数据
     * @param stockCode 股票代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 财务数据列表
     */
    @Query("SELECT sf FROM StockFinancial sf WHERE sf.stockCode = :stockCode " +
           "AND sf.reportDate BETWEEN :startDate AND :endDate ORDER BY sf.reportDate DESC")
    List<StockFinancial> findByStockCodeAndDateRange(
        @Param("stockCode") String stockCode,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}