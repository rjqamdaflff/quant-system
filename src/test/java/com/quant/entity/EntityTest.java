package com.quant.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Entity 类测试
 */
class EntityTest {

    @Test
    @DisplayName("StockInfo Builder测试")
    void testStockInfoBuilder() {
        StockInfo info = StockInfo.builder()
            .stockCode("000001")
            .stockName("平安银行")
            .industry("银行")
            .market("sz")
            .listDate(LocalDate.of(1991, 4, 3))
            .build();

        assertEquals("000001", info.getStockCode());
        assertEquals("平安银行", info.getStockName());
        assertEquals("银行", info.getIndustry());
        assertEquals("sz", info.getMarket());
        assertEquals(LocalDate.of(1991, 4, 3), info.getListDate());
    }

    @Test
    @DisplayName("StockInfo PrePersist回调")
    void testStockInfoPrePersist() {
        StockInfo info = new StockInfo();
        info.setStockCode("000001");
        info.onCreate();

        assertNotNull(info.getCreatedAt());
        assertNotNull(info.getUpdatedAt());
    }

    @Test
    @DisplayName("StockDaily Builder测试")
    void testStockDailyBuilder() {
        StockDaily daily = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.of(2025, 1, 15))
            .open(new BigDecimal("15.500"))
            .close(new BigDecimal("15.800"))
            .high(new BigDecimal("16.000"))
            .low(new BigDecimal("15.300"))
            .volume(1000000L)
            .amount(new BigDecimal("15800000.00"))
            .changePct(new BigDecimal("2.5"))
            .build();

        assertEquals("000001", daily.getStockCode());
        assertEquals(LocalDate.of(2025, 1, 15), daily.getTradeDate());
        assertEquals(new BigDecimal("15.500"), daily.getOpen());
        assertEquals(new BigDecimal("15.800"), daily.getClose());
        assertEquals(1000000L, daily.getVolume());
    }

    @Test
    @DisplayName("StockDaily PrePersist回调")
    void testStockDailyPrePersist() {
        StockDaily daily = new StockDaily();
        daily.setStockCode("000001");
        daily.onCreate();

        assertNotNull(daily.getCreatedAt());
    }

    @Test
    @DisplayName("CollectLog Builder测试")
    void testCollectLogBuilder() {
        CollectLog log = CollectLog.builder()
            .collectType("daily")
            .stockCode("000001")
            .status("success")
            .recordCount(100)
            .build();

        assertEquals("daily", log.getCollectType());
        assertEquals("000001", log.getStockCode());
        assertEquals("success", log.getStatus());
        assertEquals(100, log.getRecordCount());
    }

    @Test
    @DisplayName("CollectLog PrePersist回调")
    void testCollectLogPrePersist() {
        CollectLog log = new CollectLog();
        log.onCreate();

        assertNotNull(log.getCreatedAt());
    }
}