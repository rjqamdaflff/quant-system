package com.quant.repository;

import com.quant.entity.BacktestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 回测结果仓库接口
 *
 * 提供回测结果的CRUD操作和自定义查询方法
 */
@Repository
public interface BacktestResultRepository extends JpaRepository<BacktestResult, Long> {

    /**
     * 根据策略ID查询回测结果
     * @param strategyId 策略ID
     * @return 回测结果列表
     */
    List<BacktestResult> findByStrategyId(Long strategyId);

    /**
     * 根据股票代码查询回测结果
     * @param stockCode 股票代码
     * @return 回测结果列表
     */
    List<BacktestResult> findByStockCode(String stockCode);

    /**
     * 查询指定策略的最新回测结果
     * @param strategyId 策略ID
     * @return 最新回测结果
     */
    @Query("SELECT br FROM BacktestResult br WHERE br.strategyId = :strategyId " +
           "ORDER BY br.backtestTime DESC LIMIT 1")
    BacktestResult findLatestByStrategyId(@Param("strategyId") Long strategyId);

    /**
     * 查询指定时间范围内的回测结果
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 回测结果列表
     */
    @Query("SELECT br FROM BacktestResult br WHERE br.backtestTime BETWEEN :startTime AND :endTime " +
           "ORDER BY br.backtestTime DESC")
    List<BacktestResult> findByTimeRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 根据回测状态查询
     * @param status 回测状态
     * @return 回测结果列表
     */
    List<BacktestResult> findByBacktestStatus(String status);

    /**
     * 查询年化收益率最高的回测结果
     * @param limit 返回数量
     * @return 回测结果列表
     */
    @Query("SELECT br FROM BacktestResult br WHERE br.backtestStatus = 'COMPLETED' " +
           "ORDER BY br.annualReturn DESC LIMIT :limit")
    List<BacktestResult> findTopPerforming(@Param("limit") int limit);

    /**
     * 查询指定股票和日期范围的回测结果
     * @param stockCode 股票代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 回测结果列表
     */
    @Query("SELECT br FROM BacktestResult br WHERE br.stockCode = :stockCode " +
           "AND br.startDate >= :startDate AND br.endDate <= :endDate " +
           "ORDER BY br.backtestTime DESC")
    List<BacktestResult> findByStockCodeAndDateRange(
        @Param("stockCode") String stockCode,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}