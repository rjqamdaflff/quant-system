package com.quant.repository;

import com.quant.entity.StockMinute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 股票分钟线数据仓库接口
 *
 * 提供分钟线数据的CRUD操作和自定义查询方法
 */
@Repository
public interface StockMinuteRepository extends JpaRepository<StockMinute, Long> {

    /**
     * 根据股票代码查询分钟线数据
     * @param stockCode 股票代码
     * @return 分钟线数据列表
     */
    List<StockMinute> findByStockCode(String stockCode);

    /**
     * 根据股票代码和周期类型查询
     * @param stockCode 股票代码
     * @param periodType 周期类型
     * @return 分钟线数据列表
     */
    List<StockMinute> findByStockCodeAndPeriodType(String stockCode, Integer periodType);

    /**
     * 根据股票代码和时间范围查询
     * @param stockCode 股票代码
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分钟线数据列表
     */
    @Query("SELECT sm FROM StockMinute sm WHERE sm.stockCode = :stockCode " +
           "AND sm.minuteTime BETWEEN :startTime AND :endTime ORDER BY sm.minuteTime ASC")
    List<StockMinute> findByStockCodeAndTimeRange(
        @Param("stockCode") String stockCode,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查询指定股票最新的分钟线数据
     * @param stockCode 股票代码
     * @param periodType 周期类型
     * @return 最新的分钟线数据
     */
    @Query("SELECT sm FROM StockMinute sm WHERE sm.stockCode = :stockCode " +
           "AND sm.periodType = :periodType ORDER BY sm.minuteTime DESC LIMIT 1")
    StockMinute findLatestByStockCodeAndPeriodType(
        @Param("stockCode") String stockCode,
        @Param("periodType") Integer periodType
    );

    /**
     * 删除指定时间之前的分钟线数据（数据清理用）
     * @param stockCode 股票代码
     * @param beforeTime 截止时间
     * @return 删除的记录数
     */
    long deleteByStockCodeAndMinuteTimeBefore(String stockCode, LocalDateTime beforeTime);
}