package com.quant.repository;

import com.quant.entity.SignalAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 信号提醒仓库接口
 *
 * 提供信号提醒的CRUD操作和自定义查询方法
 */
@Repository
public interface SignalAlertRepository extends JpaRepository<SignalAlert, Long> {

    /**
     * 根据股票代码查询信号
     * @param stockCode 股票代码
     * @return 信号列表
     */
    List<SignalAlert> findByStockCode(String stockCode);

    /**
     * 根据信号类型查询
     * @param signalType 信号类型
     * @return 信号列表
     */
    List<SignalAlert> findBySignalType(String signalType);

    /**
     * 根据提醒状态查询
     * @param alertStatus 提醒状态
     * @return 信号列表
     */
    List<SignalAlert> findByAlertStatus(String alertStatus);

    /**
     * 查询待发送的信号
     * @return 待发送信号列表
     */
    @Query("SELECT sa FROM SignalAlert sa WHERE sa.alertStatus = 'PENDING' " +
           "ORDER BY sa.signalTime ASC")
    List<SignalAlert> findPendingSignals();

    /**
     * 查询指定股票最近的信号
     * @param stockCode 股票代码
     * @param limit 数量限制
     * @return 信号列表
     */
    @Query("SELECT sa FROM SignalAlert sa WHERE sa.stockCode = :stockCode " +
           "ORDER BY sa.signalTime DESC LIMIT :limit")
    List<SignalAlert> findRecentByStockCode(
        @Param("stockCode") String stockCode,
        @Param("limit") int limit
    );

    /**
     * 查询指定时间范围内的信号
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 信号列表
     */
    @Query("SELECT sa FROM SignalAlert sa WHERE sa.signalTime BETWEEN :startTime AND :endTime " +
           "ORDER BY sa.signalTime DESC")
    List<SignalAlert> findByTimeRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 根据策略ID查询信号
     * @param strategyId 策略ID
     * @return 信号列表
     */
    List<SignalAlert> findByStrategyIdOrderBySignalTimeDesc(Long strategyId);

    /**
     * 统计指定状态信号的数量
     * @param alertStatus 提醒状态
     * @return 数量
     */
    long countByAlertStatus(String alertStatus);
}