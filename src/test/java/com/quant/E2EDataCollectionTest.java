package com.quant;

import com.quant.entity.StockDaily;
import com.quant.entity.StockInfo;
import com.quant.repository.StockDailyRepository;
import com.quant.repository.StockInfoRepository;
import com.quant.service.DataValidationService;
import com.quant.service.StockDataCollectorService;
import com.quant.service.StockDataCollectorService.CollectResult;
import com.quant.service.StockDataCollectorService.DataSourceStatus;
import com.quant.service.StockDataCollectorService.BatchCollectResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E端到端集成测试
 * 验证完整的数据采集流程
 *
 * 注意：这些测试需要真实的数据源连接
 * 如果网络不可用，测试将被跳过
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class E2EDataCollectionTest {

    @Autowired
    private StockDataCollectorService collectorService;

    @Autowired
    private DataValidationService validationService;

    @Autowired
    private StockInfoRepository stockInfoRepository;

    @Autowired
    private StockDailyRepository stockDailyRepository;

    private static boolean dataSourceAvailable = false;

    @BeforeEach
    void checkDataSource() {
        // 只在第一次检查数据源
        if (!dataSourceAvailable) {
            try {
                DataSourceStatus status = collectorService.testDataSourceConnection();
                dataSourceAvailable = status.akshareAvailable() || status.baostockAvailable();
                System.out.println("数据源状态: " + status.message());
            } catch (Exception e) {
                System.out.println("数据源检查失败: " + e.getMessage());
                dataSourceAvailable = false;
            }
        }
    }

    // ========== E2E-001: 全量采集流程测试 ==========

    @Test
    @DisplayName("E2E-001: 完整采集流程 - 获取列表 -> 采集数据 -> 入库 -> 验证")
    @EnabledIf("isDataSourceAvailable")
    void testFullCollectionFlow() {
        // Step 1: 获取股票列表
        List<StockInfo> stockList = collectorService.getAllStockList();
        assertFalse(stockList.isEmpty(), "股票列表不应为空");
        System.out.println("Step 1: 获取股票列表 " + stockList.size() + " 只");

        // Step 2: 采集单只股票数据
        String testStockCode = stockList.get(0).getStockCode();
        CollectResult result = collectorService.collectDailyData(
            testStockCode,
            LocalDate.now().minusDays(30),
            LocalDate.now()
        );

        assertTrue(result.success(), "采集应成功");
        System.out.println("Step 2: 采集股票 " + testStockCode + " 成功，记录数: " + result.count());

        // Step 3: 验证数据入库
        List<StockDaily> savedData = stockDailyRepository.findByStockCodeOrderByTradeDateDesc(testStockCode);
        assertFalse(savedData.isEmpty(), "应有入库数据");
        System.out.println("Step 3: 入库验证通过，记录数: " + savedData.size());

        // Step 4: 数据验证
        for (StockDaily daily : savedData) {
            DataValidationService.ValidationResult validation =
                validationService.validateSingle(daily);
            assertTrue(validation.isValid(), "数据应有效: " + validation.getIssues());
        }
        System.out.println("Step 4: 数据验证通过");
    }

    @Test
    @DisplayName("E2E-002: 批量采集股票")
    @EnabledIf("isDataSourceAvailable")
    void testBatchCollection() {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        BatchCollectResult result = collectorService.collectAllStocksDaily(startDate, endDate);

        System.out.println("批量采集结果: 总数=" + result.total() +
            ", 成功=" + result.success() + ", 失败=" + result.failed());

        assertTrue(result.total() > 0, "应有采集总数");
        assertTrue(result.getSuccessRate() > 0, "成功率应大于0");

        long totalRecords = stockDailyRepository.count();
        assertTrue(totalRecords > 0, "数据库应有记录");
        System.out.println("数据库总记录数: " + totalRecords);
    }

    // ========== 数据准确性E2E测试 ==========

    @Test
    @DisplayName("E2E-003: 数据准确性全链路验证")
    @EnabledIf("isDataSourceAvailable")
    void testDataAccuracyE2E() {
        List<StockInfo> stockList = collectorService.getAllStockList();
        if (stockList.isEmpty()) {
            System.out.println("跳过：无股票数据");
            return;
        }

        collectorService.collectDailyData(stockList.get(0).getStockCode(),
            LocalDate.now().minusDays(30), LocalDate.now());

        List<StockDaily> savedData = stockDailyRepository.findByStockCodeOrderByTradeDateDesc(
            stockList.get(0).getStockCode());

        for (StockDaily daily : savedData) {
            if (daily.getHigh() != null && daily.getLow() != null) {
                assertTrue(daily.getHigh().compareTo(daily.getLow()) >= 0, "最高价应>=最低价");
            }
            if (daily.getChangePct() != null) {
                assertTrue(daily.getChangePct().compareTo(new BigDecimal("-11")) >= 0);
                assertTrue(daily.getChangePct().compareTo(new BigDecimal("11")) <= 0);
            }
            if (daily.getVolume() != null) {
                assertTrue(daily.getVolume() >= 0, "成交量应为非负数");
            }
            if (daily.getClose() != null) {
                assertTrue(daily.getClose().compareTo(BigDecimal.ZERO) > 0, "收盘价应为正数");
            }
        }
        System.out.println("数据准确性验证通过，验证记录数: " + savedData.size());
    }

    // ========== 断点续传E2E测试 ==========

    @Test
    @DisplayName("E2E-004: 断点续传功能验证")
    @EnabledIf("isDataSourceAvailable")
    void testResumeE2E() {
        List<StockInfo> stockList = collectorService.getAllStockList();
        if (stockList.isEmpty()) {
            System.out.println("跳过：无股票数据");
            return;
        }

        String stockCode = stockList.get(0).getStockCode();

        CollectResult firstCollect = collectorService.collectDailyData(stockCode,
            LocalDate.now().minusDays(30), LocalDate.now());
        assertTrue(firstCollect.success(), "首次采集应成功");

        long firstCount = stockDailyRepository.countByStockCode(stockCode);
        System.out.println("首次采集记录数: " + firstCount);

        CollectResult secondCollect = collectorService.incrementalCollect(stockCode);
        assertTrue(secondCollect.success(), "增量采集应成功");

        System.out.println("增量采集结果: " + secondCollect.count() + " 条新数据");
    }

    // ========== 网络不可用时的测试 ==========

    @Test
    @DisplayName("E2E-005: 网络不可用时正确报告错误")
    void testNetworkUnavailableHandling() {
        if (dataSourceAvailable) {
            System.out.println("数据源可用，跳过此测试");
            return;
        }

        // 当网络不可用时，应该抛出异常而不是返回模拟数据
        assertThrows(RuntimeException.class, () -> {
            collectorService.getAllStockList();
        }, "网络不可用时应抛出异常，不应返回模拟数据");

        System.out.println("正确：网络不可用时抛出异常，拒绝模拟数据");
    }

    // ========== 静态方法用于条件判断 ==========

    static boolean isDataSourceAvailable() {
        return dataSourceAvailable;
    }
}