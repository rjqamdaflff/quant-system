package com.quant.service;

import com.quant.entity.StockDaily;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据验证和清洗服务
 *
 * <p>功能说明：</p>
 * <ul>
 *   <li>验证股票数据的准确性和完整性</li>
 *   <li>清洗异常数据，过滤无效记录</li>
 *   <li>跨数据源对比验证</li>
 * </ul>
 *
 * <p>验证规则：</p>
 * <ul>
 *   <li>必填字段检查：股票代码、交易日期不能为空</li>
 *   <li>价格合理性：价格必须为正数</li>
 *   <li>价格逻辑：最高价>=最低价，最高价>=开盘价/收盘价，最低价<=开盘价/收盘价</li>
 *   <li>涨跌幅范围：A股-11%~+11%（含ST股）</li>
 *   <li>成交量：不能为负数</li>
 * </ul>
 *
 * @author quant-system
 * @version 1.0.0
 */
@Slf4j
@Service
public class DataValidationService {

    /** 涨跌幅最小值（A股跌停极限，含ST股） */
    private static final BigDecimal CHANGE_PCT_MIN = new BigDecimal("-11");

    /** 涨跌幅最大值（A股涨停极限，含ST股） */
    private static final BigDecimal CHANGE_PCT_MAX = new BigDecimal("11");

    /** 价格最小值（必须为正数） */
    private static final BigDecimal PRICE_MIN = BigDecimal.ZERO;

    /**
     * 验证并清洗数据
     *
     * <p>遍历原始数据列表，逐条验证，过滤无效数据</p>
     *
     * @param rawData 原始数据列表
     * @return 清洗后的有效数据列表
     */
    public List<StockDaily> validateAndClean(List<StockDaily> rawData) {
        List<StockDaily> validData = new ArrayList<>();
        List<String> issues = new ArrayList<>();

        for (StockDaily daily : rawData) {
            ValidationResult result = validateSingle(daily);
            if (result.isValid()) {
                validData.add(daily);
            } else {
                issues.add(String.format("股票%s 日期%s: %s",
                    daily.getStockCode(), daily.getTradeDate(), result.getIssues()));
                log.warn("数据验证失败: {}", result.getIssues());
            }
        }

        if (!issues.isEmpty()) {
            log.info("数据清洗完成: 有效 {} 条, 异常 {} 条", validData.size(), issues.size());
        }

        return validData;
    }

    /**
     * 验证单条数据
     *
     * <p>执行所有验证规则，返回验证结果</p>
     *
     * @param daily 单条日线数据
     * @return 验证结果对象
     */
    public ValidationResult validateSingle(StockDaily daily) {
        List<String> issues = new ArrayList<>();

        // ===== 1. 必填字段检查 =====
        if (daily.getTradeDate() == null) {
            issues.add("交易日期为空");
        }
        if (daily.getStockCode() == null || daily.getStockCode().isEmpty()) {
            issues.add("股票代码为空");
        }

        // ===== 2. 价格合理性检查 =====
        if (daily.getClose() == null || daily.getClose().compareTo(PRICE_MIN) <= 0) {
            issues.add("收盘价无效: " + daily.getClose());
        }
        if (daily.getOpen() == null || daily.getOpen().compareTo(PRICE_MIN) <= 0) {
            issues.add("开盘价无效: " + daily.getOpen());
        }
        if (daily.getHigh() == null || daily.getHigh().compareTo(PRICE_MIN) <= 0) {
            issues.add("最高价无效: " + daily.getHigh());
        }
        if (daily.getLow() == null || daily.getLow().compareTo(PRICE_MIN) <= 0) {
            issues.add("最低价无效: " + daily.getLow());
        }

        // ===== 3. 价格逻辑检查 =====
        // 最高价必须>=最低价
        if (daily.getHigh() != null && daily.getLow() != null) {
            if (daily.getHigh().compareTo(daily.getLow()) < 0) {
                issues.add("最高价低于最低价");
            }
        }
        // 最高价必须>=开盘价
        if (daily.getHigh() != null && daily.getOpen() != null) {
            if (daily.getHigh().compareTo(daily.getOpen()) < 0) {
                issues.add("最高价低于开盘价");
            }
        }
        // 最高价必须>=收盘价
        if (daily.getHigh() != null && daily.getClose() != null) {
            if (daily.getHigh().compareTo(daily.getClose()) < 0) {
                issues.add("最高价低于收盘价");
            }
        }
        // 最低价必须<=开盘价
        if (daily.getLow() != null && daily.getOpen() != null) {
            if (daily.getLow().compareTo(daily.getOpen()) > 0) {
                issues.add("最低价高于开盘价");
            }
        }
        // 最低价必须<=收盘价
        if (daily.getLow() != null && daily.getClose() != null) {
            if (daily.getLow().compareTo(daily.getClose()) > 0) {
                issues.add("最低价高于收盘价");
            }
        }

        // ===== 4. 成交量检查 =====
        if (daily.getVolume() != null && daily.getVolume() < 0) {
            issues.add("成交量为负数: " + daily.getVolume());
        }

        // ===== 5. 涨跌幅范围检查 =====
        if (daily.getChangePct() != null) {
            if (daily.getChangePct().compareTo(CHANGE_PCT_MIN) < 0 ||
                daily.getChangePct().compareTo(CHANGE_PCT_MAX) > 0) {
                issues.add("涨跌幅超出合理范围: " + daily.getChangePct() + "%");
            }
        }

        return new ValidationResult(issues.isEmpty(), issues);
    }

    /**
     * 跨数据源对比验证
     *
     * <p>比较两个数据源的同一股票同一日期数据，检查一致性</p>
     * <p>允许误差：0.01%</p>
     *
     * @param data1 第一个数据源的数据
     * @param data2 第二个数据源的数据
     * @return 对比验证结果
     */
    public CrossValidationResult compareSources(StockDaily data1, StockDaily data2) {
        // 检查股票代码是否一致
        if (!data1.getStockCode().equals(data2.getStockCode())) {
            return new CrossValidationResult(false, "股票代码不一致", null);
        }
        // 检查交易日期是否一致
        if (!data1.getTradeDate().equals(data2.getTradeDate())) {
            return new CrossValidationResult(false, "交易日期不一致", null);
        }

        // 计算价格差异百分比
        BigDecimal closeDiff = calculateDiffPercent(data1.getClose(), data2.getClose());
        BigDecimal openDiff = calculateDiffPercent(data1.getOpen(), data2.getOpen());

        List<String> differences = new ArrayList<>();
        boolean valid = true;

        // 允许误差：0.01%
        BigDecimal tolerance = new BigDecimal("0.01");

        if (closeDiff.abs().compareTo(tolerance) > 0) {
            valid = false;
            differences.add(String.format("收盘价差异 %.4f%%", closeDiff));
        }
        if (openDiff.abs().compareTo(tolerance) > 0) {
            valid = false;
            differences.add(String.format("开盘价差异 %.4f%%", openDiff));
        }

        return new CrossValidationResult(valid,
            valid ? "数据一致" : "数据存在差异",
            differences);
    }

    /**
     * 计算两个价格的差异百分比
     *
     * @param v1 第一个价格
     * @param v2 第二个价格
     * @return 差异百分比（v2相对于v1的变化）
     */
    private BigDecimal calculateDiffPercent(BigDecimal v1, BigDecimal v2) {
        if (v1 == null || v2 == null || v1.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return v2.subtract(v1).divide(v1, 6, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"));
    }

    // ==================== 内部类 ====================

    /**
     * 验证结果记录类
     *
     * @param isValid 是否验证通过
     * @param issues 问题列表（验证失败时包含具体问题）
     */
    public record ValidationResult(boolean isValid, List<String> issues) {

        /**
         * 获取问题描述字符串
         *
         * @return 问题列表的字符串表示
         */
        public String getIssues() {
            return issues.isEmpty() ? "无问题" : String.join("; ", issues);
        }
    }

    /**
     * 跨数据源对比验证结果记录类
     */
    public record CrossValidationResult(boolean valid, String message, List<String> differences) {}
}