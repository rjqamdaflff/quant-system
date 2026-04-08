package com.quant.service;

import com.quant.entity.StockDaily;
import com.quant.entity.StockInfo;
import com.quant.repository.StockDailyRepository;
import com.quant.repository.StockInfoRepository;
import com.quant.service.StockDataCollectorService.CollectResult;
import com.quant.service.StockDataCollectorService.DataSourceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 数据采集服务测试类
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockDataCollectorServiceTest {

    @Mock
    private StockInfoRepository stockInfoRepository;

    @Mock
    private StockDailyRepository stockDailyRepository;

    @Mock
    private DataValidationService validationService;

    @Mock
    private CollectLogService collectLogService;

    @InjectMocks
    private StockDataCollectorService collectorService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(collectorService, "retryTimes", 3);
        ReflectionTestUtils.setField(collectorService, "scriptPath", "scripts/data_collector.py");
        ReflectionTestUtils.setField(collectorService, "pythonPath", "python3");
    }

    @Test
    @DisplayName("UT-001: 测试数据源连接状态")
    void testDataSourceConnection() {
        // When
        DataSourceStatus status = collectorService.testDataSourceConnection();

        // Then
        assertNotNull(status, "应返回连接状态");
        assertNotNull(status.message(), "应包含状态信息");
    }

    @Test
    @DisplayName("UT-002: 验证保存日线数据功能")
    void testSaveDailyData() {
        // Given
        StockDaily daily = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.of(2025, 1, 15))
            .open(new BigDecimal("15.500"))
            .close(new BigDecimal("15.800"))
            .high(new BigDecimal("16.000"))
            .low(new BigDecimal("15.300"))
            .volume(1000000L)
            .build();

        when(stockDailyRepository.existsByStockCodeAndTradeDate(any(), any())).thenReturn(false);
        when(stockDailyRepository.save(any())).thenReturn(daily);

        // When
        int count = collectorService.saveDailyData(List.of(daily));

        // Then
        assertEquals(1, count, "应保存1条数据");
        verify(stockDailyRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("UT-003: 验证重复数据不保存")
    void testSkipDuplicateData() {
        // Given
        StockDaily daily = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.of(2025, 1, 15))
            .build();

        when(stockDailyRepository.existsByStockCodeAndTradeDate(any(), any())).thenReturn(true);

        // When
        int count = collectorService.saveDailyData(List.of(daily));

        // Then
        assertEquals(0, count, "重复数据不应保存");
        verify(stockDailyRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-004: 增量采集逻辑验证")
    void testIncrementalCollectLogic() {
        // Given - 模拟已有最新数据
        when(stockDailyRepository.findLatestTradeDateByStockCode("000001"))
            .thenReturn(Optional.of(LocalDate.now()));
        when(stockDailyRepository.existsByStockCodeAndTradeDate(any(), any())).thenReturn(false);

        // When
        CollectResult result = collectorService.incrementalCollect("000001");

        // Then - 数据已是最新，应返回成功但0条记录
        assertTrue(result.success(), "增量采集应成功");
        assertEquals(0, result.count(), "已是最新的数据应返回0条");
    }
}