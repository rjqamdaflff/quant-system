package com.quant.service;

import com.quant.entity.CollectLog;
import com.quant.repository.CollectLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 数据采集日志服务
 *
 * <p>功能说明：</p>
 * <ul>
 *   <li>记录数据采集过程中的成功/失败状态</li>
 *   <li>支持按类型统计采集成功率</li>
 *   <li>所有日志持久化到数据库</li>
 * </ul>
 *
 * <p>日志类型：</p>
 * <ul>
 *   <li>stock_list: 股票列表采集</li>
 *   <li>daily: 日线数据采集</li>
 *   <li>minute: 分钟线数据采集</li>
 *   <li>financial: 财务数据采集</li>
 * </ul>
 *
 * @author quant-system
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectLogService {

    /** 采集日志仓库 */
    private final CollectLogRepository collectLogRepository;

    /**
     * 记录采集成功日志
     *
     * @param collectType 采集类型（stock_list/daily/minute/financial）
     * @param stockCode 股票代码（可选，列表采集时为null）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param recordCount 采集记录数
     */
    @Transactional
    public void logSuccess(String collectType, String stockCode,
                           LocalDateTime startTime, LocalDateTime endTime, int recordCount) {
        CollectLog logEntity = CollectLog.builder()
            .collectType(collectType)
            .stockCode(stockCode)
            .startTime(startTime)
            .endTime(endTime)
            .status("success")
            .recordCount(recordCount)
            .build();
        collectLogRepository.save(logEntity);
        log.info("采集成功: 类型={}, 股票={}, 记录数={}", collectType, stockCode, recordCount);
    }

    /**
     * 记录采集失败日志
     *
     * @param collectType 采集类型
     * @param stockCode 股票代码（可选）
     * @param errorMsg 错误信息
     */
    @Transactional
    public void logError(String collectType, String stockCode, String errorMsg) {
        CollectLog logEntity = CollectLog.builder()
            .collectType(collectType)
            .stockCode(stockCode)
            .startTime(LocalDateTime.now())
            .status("failed")
            .errorMsg(errorMsg)
            .build();
        collectLogRepository.save(logEntity);
        log.error("采集失败: 类型={}, 股票={}, 错误={}", collectType, stockCode, errorMsg);
    }

    /**
     * 统计某类型采集成功的次数
     *
     * @param collectType 采集类型
     * @return 成功次数
     */
    public long getSuccessCount(String collectType) {
        return collectLogRepository.countByTypeAndStatus(collectType, "success");
    }
}