package com.quant.repository;

import com.quant.entity.StrategyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 策略配置仓库接口
 *
 * 提供策略配置的CRUD操作和自定义查询方法
 */
@Repository
public interface StrategyConfigRepository extends JpaRepository<StrategyConfig, Long> {

    /**
     * 根据用户ID查询策略配置
     * @param userId 用户ID
     * @return 策略配置列表
     */
    List<StrategyConfig> findByUserId(Long userId);

    /**
     * 根据状态查询策略配置
     * @param status 状态
     * @return 策略配置列表
     */
    List<StrategyConfig> findByStatus(String status);

    /**
     * 查询用户的所有活跃策略
     * @param userId 用户ID
     * @return 活跃策略列表
     */
    @Query("SELECT sc FROM StrategyConfig sc WHERE sc.userId = :userId " +
           "AND sc.status = 'ACTIVE' ORDER BY sc.updateTime DESC")
    List<StrategyConfig> findActiveStrategiesByUserId(@Param("userId") Long userId);

    /**
     * 根据策略类型查询
     * @param strategyType 策略类型
     * @return 策略配置列表
     */
    List<StrategyConfig> findByStrategyType(String strategyType);

    /**
     * 查询所有启用提醒的活跃策略
     * @return 策略配置列表
     */
    @Query("SELECT sc FROM StrategyConfig sc WHERE sc.status = 'ACTIVE' " +
           "AND sc.alertEnabled = true")
    List<StrategyConfig> findAllActiveWithAlert();

    /**
     * 根据用户ID和策略名称查询
     * @param userId 用户ID
     * @param strategyName 策略名称
     * @return 策略配置
     */
    StrategyConfig findByUserIdAndStrategyName(Long userId, String strategyName);
}