package com.quant.controller;

import com.quant.entity.StockDaily;
import com.quant.entity.StockInfo;
import com.quant.repository.StockDailyRepository;
import com.quant.repository.StockInfoRepository;
import com.quant.service.DataValidationService;
import com.quant.service.StockDataCollectorService;
import com.quant.service.StockDataCollectorService.BatchCollectResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataCollectController.class)
@ActiveProfiles("test")
class DataCollectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockDataCollectorService collectorService;

    @MockBean
    private DataValidationService validationService;

    @MockBean
    private StockInfoRepository stockInfoRepository;

    @MockBean
    private StockDailyRepository stockDailyRepository;

    private StockInfo testStock;
    private StockDaily testDaily;

    @BeforeEach
    void setUp() {
        testStock = StockInfo.builder()
            .stockCode("000001")
            .stockName("平安银行")
            .industry("银行")
            .market("sz")
            .build();

        testDaily = StockDaily.builder()
            .stockCode("000001")
            .tradeDate(LocalDate.of(2025, 1, 15))
            .open(new BigDecimal("15.500"))
            .close(new BigDecimal("15.800"))
            .high(new BigDecimal("16.000"))
            .low(new BigDecimal("15.300"))
            .volume(1000000L)
            .build();
    }

    @Test
    @DisplayName("GET /api/collect/stocks - 获取股票列表")
    void testGetStockList() throws Exception {
        when(collectorService.getAllStockList()).thenReturn(List.of(testStock));

        mockMvc.perform(get("/api/collect/stocks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.stocks[0].stockCode").value("000001"));
    }

    @Test
    @DisplayName("GET /api/collect/status - 获取系统状态")
    void testGetStatus() throws Exception {
        when(stockInfoRepository.count()).thenReturn(10L);
        when(stockDailyRepository.count()).thenReturn(1000L);

        mockMvc.perform(get("/api/collect/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stockCount").value(10))
            .andExpect(jsonPath("$.dailyRecordCount").value(1000))
            .andExpect(jsonPath("$.status").value("running"));
    }

    @Test
    @DisplayName("GET /api/collect/daily/{stockCode} - 查询日线数据")
    void testGetDailyData() throws Exception {
        when(stockDailyRepository.findRecentByStockCode("000001", 30))
            .thenReturn(List.of(testDaily));

        mockMvc.perform(get("/api/collect/daily/000001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stockCode").value("000001"))
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("GET /api/collect/validate/{stockCode} - 验证数据")
    void testValidateData() throws Exception {
        when(stockDailyRepository.findByStockCodeOrderByTradeDateDesc("000001"))
            .thenReturn(List.of(testDaily));
        when(stockDailyRepository.findAbnormalChangePct()).thenReturn(List.of());
        when(stockDailyRepository.findNegativeVolume()).thenReturn(List.of());
        when(stockDailyRepository.findInvalidPrices()).thenReturn(List.of());
        when(validationService.validateSingle(any()))
            .thenReturn(new DataValidationService.ValidationResult(true, List.of()));

        mockMvc.perform(get("/api/collect/validate/000001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stockCode").value("000001"))
            .andExpect(jsonPath("$.validRecords").value(1));
    }

    @Test
    @DisplayName("POST /api/collect/daily/{stockCode} - 采集单只股票")
    void testCollectDaily() throws Exception {
        when(collectorService.collectDailyData(any(), any(), any()))
            .thenReturn(StockDataCollectorService.CollectResult.success("000001", 10));

        mockMvc.perform(post("/api/collect/daily/000001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.recordCount").value(10));
    }

    @Test
    @DisplayName("POST /api/collect/daily/batch - 批量采集")
    void testBatchCollect() throws Exception {
        BatchCollectResult mockResult = new BatchCollectResult(10, 8, 2, List.of("000005: 失败"));
        when(collectorService.collectAllStocksDaily(any(), any())).thenReturn(mockResult);

        mockMvc.perform(post("/api/collect/daily/batch"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(10))
            .andExpect(jsonPath("$.success").value(8))
            .andExpect(jsonPath("$.failed").value(2));
    }
}