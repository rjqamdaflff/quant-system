package com.quant.service;

import com.quant.entity.StockDaily;
import com.quant.service.DataValidationService.CrossValidationResult;
import com.quant.service.DataValidationService.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据验证服务测试类
 * US-004: 数据准确性验证
 */
class DataValidationServiceTest {

    private DataValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new DataValidationService();
    }

    // ========== DV-001: 跨数据源对比测试 ==========

    @Test
    @DisplayName("DV-001: 跨数据源对比 - 数据一致")
    void testCrossSourceValidationMatch() {
        // Given - 两个数据源的相同数据（误差<0.01%）
        StockDaily data1 = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.of(2025, 1, 15))
            .open(new BigDecimal("15.500"))
            .close(new BigDecimal("15.800"))
            .build();

        StockDaily data2 = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.of(2025, 1, 15))
            .open(new BigDecimal("15.501"))  // 差异0.006%
            .close(new BigDecimal("15.801")) // 差异0.006%
            .build();

        // When
        CrossValidationResult result = validationService.compareSources(data1, data2);

        // Then
        assertTrue(result.valid(), "误差<0.01%应通过验证");
        assertEquals("数据一致", result.message());
    }

    @Test
    @DisplayName("DV-001: 跨数据源对比 - 数据不一致")
    void testCrossSourceValidationMismatch() {
        // Given - 两个数据源差异超过阈值
        StockDaily data1 = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.of(2025, 1, 15))
            .open(new BigDecimal("15.500"))
            .close(new BigDecimal("15.800"))
            .build();

        StockDaily data2 = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.of(2025, 1, 15))
            .open(new BigDecimal("16.000"))  // 差异>3%
            .close(new BigDecimal("16.500")) // 差异>4%
            .build();

        // When
        CrossValidationResult result = validationService.compareSources(data1, data2);

        // Then
        assertFalse(result.valid(), "误差>0.01%应不通过验证");
        assertNotNull(result.differences(), "应返回差异详情");
    }

    // ========== DV-002: 价格合理性验证 ==========

    @Test
    @DisplayName("DV-002: 价格逻辑验证 - 最高价>=最低价")
    void testPriceLogicHighGreaterEqualLow() {
        // Given - 正确的价格关系
        StockDaily daily = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.now())
            .open(new BigDecimal("15.500"))
            .close(new BigDecimal("15.800"))
            .high(new BigDecimal("16.000"))  // 最高
            .low(new BigDecimal("15.300"))   // 最低
            .volume(1000000L)
            .changePct(new BigDecimal("2.0"))
            .build();

        // When
        ValidationResult result = validationService.validateSingle(daily);

        // Then
        assertTrue(result.isValid(), "正确价格关系应通过验证");
    }

    @Test
    @DisplayName("DV-002: 价格逻辑验证 - 最高价低于最低价(异常)")
    void testPriceLogicHighLowerThanLow() {
        // Given - 异常价格关系
        StockDaily daily = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.now())
            .open(new BigDecimal("15.500"))
            .close(new BigDecimal("15.800"))
            .high(new BigDecimal("15.000"))  // 最高 < 最低
            .low(new BigDecimal("16.000"))   // 最低 > 最高
            .volume(1000000L)
            .build();

        // When
        ValidationResult result = validationService.validateSingle(daily);

        // Then
        assertFalse(result.isValid(), "异常价格关系应不通过验证");
        assertTrue(result.issues().stream().anyMatch(i -> i.contains("最高价低于最低价")));
    }

    // ========== DV-003: 涨跌幅范围验证 ==========

    @Test
    @DisplayName("DV-003: 涨跌幅范围验证 - 正常范围")
    void testChangePctNormalRange() {
        // Given - 正常涨跌幅
        StockDaily daily = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.now())
            .open(new BigDecimal("15.500"))
            .close(new BigDecimal("15.800"))
            .high(new BigDecimal("16.000"))
            .low(new BigDecimal("15.300"))
            .volume(1000000L)
            .changePct(new BigDecimal("5.0"))  // +5% 正常
            .build();

        // When
        ValidationResult result = validationService.validateSingle(daily);

        // Then
        assertTrue(result.isValid(), "正常涨跌幅应通过验证");
    }

    @Test
    @DisplayName("DV-003: 涨跌幅范围验证 - 超出范围")
    void testChangePctOutOfRange() {
        // Given - 超出范围涨跌幅
        StockDaily daily = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.now())
            .open(new BigDecimal("15.500"))
            .close(new BigDecimal("18.000"))
            .high(new BigDecimal("18.000"))
            .low(new BigDecimal("15.300"))
            .volume(1000000L)
            .changePct(new BigDecimal("15.0"))  // +15% 超出A股限制
            .build();

        // When
        ValidationResult result = validationService.validateSingle(daily);

        // Then
        assertFalse(result.isValid(), "超出范围涨跌幅应不通过验证");
        assertTrue(result.issues().stream().anyMatch(i -> i.contains("涨跌幅超出合理范围")));
    }

    // ========== DV-004: 成交量验证 ==========

    @Test
    @DisplayName("DV-004: 成交量验证 - 负数检测")
    void testNegativeVolume() {
        // Given - 负成交量
        StockDaily daily = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.now())
            .open(new BigDecimal("15.500"))
            .close(new BigDecimal("15.800"))
            .high(new BigDecimal("16.000"))
            .low(new BigDecimal("15.300"))
            .volume(-1000L)  // 负数
            .changePct(new BigDecimal("2.0"))
            .build();

        // When
        ValidationResult result = validationService.validateSingle(daily);

        // Then
        assertFalse(result.isValid(), "负成交量应不通过验证");
        assertTrue(result.issues().stream().anyMatch(i -> i.contains("成交量为负数")));
    }

    @Test
    @DisplayName("DV-004: 成交量验证 - 正数通过")
    void testPositiveVolume() {
        // Given - 正成交量
        StockDaily daily = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.now())
            .open(new BigDecimal("15.500"))
            .close(new BigDecimal("15.800"))
            .high(new BigDecimal("16.000"))
            .low(new BigDecimal("15.300"))
            .volume(1000000L)  // 正数
            .changePct(new BigDecimal("2.0"))
            .build();

        // When
        ValidationResult result = validationService.validateSingle(daily);

        // Then
        assertTrue(result.isValid(), "正成交量应通过验证");
    }

    // ========== 数据清洗测试 ==========

    @Test
    @DisplayName("批量数据清洗 - 混合数据")
    void testBatchValidateAndClean() {
        // Given - 混合有效和无效数据
        StockDaily valid1 = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.now())
            .open(new BigDecimal("15.500"))
            .close(new BigDecimal("15.800"))
            .high(new BigDecimal("16.000"))
            .low(new BigDecimal("15.300"))
            .volume(1000000L)
            .changePct(new BigDecimal("2.0"))
            .build();

        StockDaily invalid = StockDaily.builder()
            .stockCode("000002")
            .tradeDate(LocalDate.now())
            .open(new BigDecimal("15.500"))
            .close(new BigDecimal("15.800"))
            .high(new BigDecimal("14.000"))  // 异常
            .low(new BigDecimal("16.000"))
            .volume(-1000L)  // 异常
            .changePct(new BigDecimal("20.0"))  // 异常
            .build();

        StockDaily valid2 = StockDaily.builder()
            .stockCode("000003")
            .tradeDate(LocalDate.now().minusDays(1))
            .open(new BigDecimal("20.000"))
            .close(new BigDecimal("20.500"))
            .high(new BigDecimal("21.000"))
            .low(new BigDecimal("19.500"))
            .volume(2000000L)
            .changePct(new BigDecimal("-1.5"))
            .build();

        // When
        List<StockDaily> result = validationService.validateAndClean(List.of(valid1, invalid, valid2));

        // Then
        assertEquals(2, result.size(), "应过滤掉1条无效数据");
        assertTrue(result.stream().allMatch(d -> d.getStockCode().equals("000001") || d.getStockCode().equals("000003")));
    }
}