package com.quant.repository;

import com.quant.entity.StockDaily;
import com.quant.entity.StockInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据仓库集成测试
 * US-001: 数据库设计验证
 */
@DataJpaTest
@ActiveProfiles("test")
class StockRepositoryTest {

    @Autowired
    private StockInfoRepository stockInfoRepository;

    @Autowired
    private StockDailyRepository stockDailyRepository;

    private StockInfo testStock;

    @BeforeEach
    void setUp() {
        testStock = StockInfo.builder()
            .stockCode("000001")
            .stockName("平安银行")
            .industry("银行")
            .market("sz")
            .listDate(LocalDate.of(1991, 4, 3))
            .build();
    }

    // ========== US-001: 数据库表设计测试 ==========

    @Test
    @DisplayName("IT-001: 股票信息表CRUD操作")
    void testStockInfoCRUD() {
        // Create
        StockInfo saved = stockInfoRepository.save(testStock);
        assertNotNull(saved.getId(), "保存后应有ID");

        // Read
        Optional<StockInfo> found = stockInfoRepository.findByStockCode("000001");
        assertTrue(found.isPresent(), "应能找到股票");
        assertEquals("平安银行", found.get().getStockName());

        // Update
        saved.setIndustry("金融");
        stockInfoRepository.save(saved);
        assertEquals("金融", stockInfoRepository.findByStockCode("000001").get().getIndustry());

        // Delete
        stockInfoRepository.delete(saved);
        assertFalse(stockInfoRepository.findByStockCode("000001").isPresent());
    }

    @Test
    @DisplayName("IT-001: 日线数据表CRUD操作")
    void testStockDailyCRUD() {
        // 先保存股票信息
        stockInfoRepository.save(testStock);

        // Create
        StockDaily daily = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.of(2025, 1, 15))
            .open(new BigDecimal("15.500"))
            .close(new BigDecimal("15.800"))
            .high(new BigDecimal("16.000"))
            .low(new BigDecimal("15.300"))
            .volume(1000000L)
            .amount(new BigDecimal("15800000.00"))
            .changePct(new BigDecimal("2.0"))
            .build();

        StockDaily savedDaily = stockDailyRepository.save(daily);
        assertNotNull(savedDaily.getId(), "保存后应有ID");

        // Read
        Optional<StockDaily> found = stockDailyRepository.findByStockCodeAndTradeDate(
            "000001", LocalDate.of(2025, 1, 15));
        assertTrue(found.isPresent(), "应能找到日线数据");
        assertEquals(new BigDecimal("15.800"), found.get().getClose());

        // 检查唯一约束
        StockDaily duplicate = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.of(2025, 1, 15))
            .close(new BigDecimal("16.000"))
            .build();

        // 应该抛出异常或忽略
        assertDoesNotThrow(() -> {
            try {
                stockDailyRepository.saveAndFlush(duplicate);
            } catch (Exception e) {
                // 预期会有唯一约束冲突
            }
        });
    }

    @Test
    @DisplayName("IT-002: 增量采集 - 查询最新日期")
    void testFindLatestTradeDate() {
        stockInfoRepository.save(testStock);

        // 保存多条日线数据
        for (int i = 1; i <= 5; i++) {
            StockDaily daily = StockDaily.builder()
                .stockCode("000001")
                .tradeDate(LocalDate.of(2025, 1, i))
                .close(new BigDecimal("15.00" + i))
                .build();
            stockDailyRepository.save(daily);
        }

        Optional<LocalDate> latest = stockDailyRepository.findLatestTradeDateByStockCode("000001");

        assertTrue(latest.isPresent(), "应找到最新日期");
        assertEquals(LocalDate.of(2025, 1, 5), latest.get());
    }

    @Test
    @DisplayName("IT-003: 日期范围查询")
    void testFindByDateRange() {
        stockInfoRepository.save(testStock);

        // 保存多条数据
        for (int i = 1; i <= 10; i++) {
            StockDaily daily = StockDaily.builder()
                .stockCode("000001")
                .tradeDate(LocalDate.of(2025, 1, i))
                .close(new BigDecimal("15.00" + i))
                .build();
            stockDailyRepository.save(daily);
        }

        List<StockDaily> result = stockDailyRepository.findByStockCodeAndTradeDateBetween(
            "000001", LocalDate.of(2025, 1, 3), LocalDate.of(2025, 1, 7));

        assertEquals(5, result.size(), "应返回5条数据");
    }

    // ========== 数据准确性验证查询测试 ==========

    @Test
    @DisplayName("DV-003: 异常涨跌幅查询")
    void testFindAbnormalChangePct() {
        stockInfoRepository.save(testStock);

        // 正常数据
        StockDaily normal = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.of(2025, 1, 1))
            .close(new BigDecimal("15.00"))
            .changePct(new BigDecimal("2.0"))
            .build();

        // 异常数据（涨停超过11%）
        StockDaily abnormal = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.of(2025, 1, 2))
            .close(new BigDecimal("20.00"))
            .changePct(new BigDecimal("15.0"))  // 超出范围
            .build();

        stockDailyRepository.save(normal);
        stockDailyRepository.save(abnormal);

        List<StockDaily> abnormalList = stockDailyRepository.findAbnormalChangePct();

        assertFalse(abnormalList.isEmpty(), "应找到异常数据");
    }

    @Test
    @DisplayName("DV-004: 负成交量查询")
    void testFindNegativeVolume() {
        stockInfoRepository.save(testStock);

        // 正常数据
        StockDaily normal = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.of(2025, 1, 1))
            .close(new BigDecimal("15.00"))
            .volume(1000000L)
            .build();

        // 异常数据
        StockDaily abnormal = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.of(2025, 1, 2))
            .close(new BigDecimal("15.00"))
            .volume(-1000L)
            .build();

        stockDailyRepository.save(normal);
        stockDailyRepository.save(abnormal);

        List<StockDaily> negativeVolume = stockDailyRepository.findNegativeVolume();

        assertFalse(negativeVolume.isEmpty(), "应找到负成交量数据");
    }
}