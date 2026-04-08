package com.quant.service;

import com.quant.entity.StockDaily;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 技术指标计算服务
 *
 * 提供常用技术指标的计算功能：
 * - MA: 移动平均线（5/10/20/60日）
 * - MACD: 指数平滑异同移动平均线
 * - KDJ: 随机指标
 * - RSI: 相对强弱指标
 * - BOLL: 布林带
 *
 * 所有指标计算基于真实交易数据，不使用模拟数据
 */
@Service
public class IndicatorCalculator {

    /**
     * 计算移动平均线
     *
     * @param dailyList 日线数据列表（按日期升序）
     * @param period 周期（如5、10、20、60）
     * @return MA值列表，前period-1个元素为null
     */
    public BigDecimal[] calculateMA(List<StockDaily> dailyList, int period) {
        if (dailyList == null || dailyList.size() < period) {
            return new BigDecimal[0];
        }

        int size = dailyList.size();
        BigDecimal[] maValues = new BigDecimal[size];

        for (int i = 0; i < size; i++) {
            if (i < period - 1) {
                maValues[i] = null;
                continue;
            }

            BigDecimal sum = BigDecimal.ZERO;
            for (int j = i - period + 1; j <= i; j++) {
                sum = sum.add(dailyList.get(j).getClose());
            }
            maValues[i] = sum.divide(BigDecimal.valueOf(period), 3, BigDecimal.ROUND_HALF_UP);
        }

        return maValues;
    }

    /**
     * 计算EMA（指数移动平均）
     */
    private BigDecimal calculateEMA(BigDecimal[] values, int period, int index) {
        if (index < period - 1) {
            return null;
        }

        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));

        if (index == period - 1) {
            // 第一个EMA使用简单平均
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = 0; i < period; i++) {
                sum = sum.add(values[i]);
            }
            return sum.divide(BigDecimal.valueOf(period), 4, BigDecimal.ROUND_HALF_UP);
        }

        BigDecimal prevEMA = calculateEMA(values, period, index - 1);
        if (prevEMA == null) {
            return null;
        }

        return values[index].multiply(multiplier)
            .add(prevEMA.multiply(BigDecimal.ONE.subtract(multiplier)))
            .setScale(4, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 计算MACD指标
     *
     * @param dailyList 日线数据列表
     * @return MACDResult 包含DIF、DEA、MACD柱
     */
    public MACDResult calculateMACD(List<StockDaily> dailyList) {
        if (dailyList == null || dailyList.size() < 35) {
            return new MACDResult();
        }

        int size = dailyList.size();
        BigDecimal[] closePrices = new BigDecimal[size];
        for (int i = 0; i < size; i++) {
            closePrices[i] = dailyList.get(i).getClose();
        }

        // 计算EMA12和EMA26
        BigDecimal[] ema12 = new BigDecimal[size];
        BigDecimal[] ema26 = new BigDecimal[size];
        BigDecimal[] dif = new BigDecimal[size];

        for (int i = 0; i < size; i++) {
            ema12[i] = calculateEMA(closePrices, 12, i);
            ema26[i] = calculateEMA(closePrices, 26, i);

            if (ema12[i] != null && ema26[i] != null) {
                dif[i] = ema12[i].subtract(ema26[i]);
            }
        }

        // 计算DEA（DIF的9日EMA）
        BigDecimal[] dea = new BigDecimal[size];
        for (int i = 0; i < size; i++) {
            dea[i] = calculateEMA(dif, 9, i);
        }

        // 计算MACD柱
        BigDecimal[] macd = new BigDecimal[size];
        for (int i = 0; i < size; i++) {
            if (dif[i] != null && dea[i] != null) {
                macd[i] = dif[i].subtract(dea[i]).multiply(BigDecimal.valueOf(2));
            }
        }

        return new MACDResult(dif, dea, macd);
    }

    /**
     * 计算KDJ指标
     *
     * @param dailyList 日线数据列表
     * @param n 周期（默认9）
     * @param m1 K值平滑周期（默认3）
     * @param m2 D值平滑周期（默认3）
     * @return KDJResult 包含K、D、J值
     */
    public KDJResult calculateKDJ(List<StockDaily> dailyList, int n, int m1, int m2) {
        if (dailyList == null || dailyList.size() < n) {
            return new KDJResult();
        }

        int size = dailyList.size();
        BigDecimal[] kValues = new BigDecimal[size];
        BigDecimal[] dValues = new BigDecimal[size];
        BigDecimal[] jValues = new BigDecimal[size];

        BigDecimal prevK = BigDecimal.valueOf(50);
        BigDecimal prevD = BigDecimal.valueOf(50);

        for (int i = 0; i < size; i++) {
            if (i < n - 1) {
                kValues[i] = null;
                dValues[i] = null;
                jValues[i] = null;
                continue;
            }

            // 计算N日内的最高价和最低价
            BigDecimal highest = dailyList.get(i).getHigh();
            BigDecimal lowest = dailyList.get(i).getLow();

            for (int j = i - n + 1; j <= i; j++) {
                if (dailyList.get(j).getHigh().compareTo(highest) > 0) {
                    highest = dailyList.get(j).getHigh();
                }
                if (dailyList.get(j).getLow().compareTo(lowest) < 0) {
                    lowest = dailyList.get(j).getLow();
                }
            }

            // 计算RSV
            BigDecimal closePrice = dailyList.get(i).getClose();
            BigDecimal range = highest.subtract(lowest);
            BigDecimal rsv;

            if (range.compareTo(BigDecimal.ZERO) == 0) {
                rsv = BigDecimal.valueOf(50);
            } else {
                rsv = closePrice.subtract(lowest)
                    .divide(range, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            }

            // 计算K值
            BigDecimal alphaK = BigDecimal.ONE.divide(BigDecimal.valueOf(m1), 4, BigDecimal.ROUND_HALF_UP);
            BigDecimal k = alphaK.multiply(rsv).add(BigDecimal.ONE.subtract(alphaK).multiply(prevK));
            kValues[i] = k.setScale(2, BigDecimal.ROUND_HALF_UP);

            // 计算D值
            BigDecimal alphaD = BigDecimal.ONE.divide(BigDecimal.valueOf(m2), 4, BigDecimal.ROUND_HALF_UP);
            BigDecimal d = alphaD.multiply(k).add(BigDecimal.ONE.subtract(alphaD).multiply(prevD));
            dValues[i] = d.setScale(2, BigDecimal.ROUND_HALF_UP);

            // 计算J值
            jValues[i] = k.multiply(BigDecimal.valueOf(3))
                .subtract(d.multiply(BigDecimal.valueOf(2)))
                .setScale(2, BigDecimal.ROUND_HALF_UP);

            prevK = k;
            prevD = d;
        }

        return new KDJResult(kValues, dValues, jValues);
    }

    /**
     * 计算RSI指标
     *
     * @param dailyList 日线数据列表
     * @param period 周期（常用6、12、24）
     * @return RSI值数组
     */
    public BigDecimal[] calculateRSI(List<StockDaily> dailyList, int period) {
        if (dailyList == null || dailyList.size() < period + 1) {
            return new BigDecimal[0];
        }

        int size = dailyList.size();
        BigDecimal[] rsiValues = new BigDecimal[size];

        BigDecimal prevAvgGain = BigDecimal.ZERO;
        BigDecimal prevAvgLoss = BigDecimal.ZERO;

        for (int i = 0; i < size; i++) {
            if (i < period) {
                rsiValues[i] = null;
                continue;
            }

            // 计算价格变化
            BigDecimal change = dailyList.get(i).getClose()
                .subtract(dailyList.get(i - 1).getClose());

            BigDecimal gain = change.compareTo(BigDecimal.ZERO) > 0 ? change : BigDecimal.ZERO;
            BigDecimal loss = change.compareTo(BigDecimal.ZERO) < 0 ? change.abs() : BigDecimal.ZERO;

            if (i == period) {
                // 第一个RSI使用简单平均
                for (int j = 1; j <= period; j++) {
                    BigDecimal c = dailyList.get(j).getClose()
                        .subtract(dailyList.get(j - 1).getClose());
                    if (c.compareTo(BigDecimal.ZERO) > 0) {
                        prevAvgGain = prevAvgGain.add(c);
                    } else {
                        prevAvgLoss = prevAvgLoss.add(c.abs());
                    }
                }
                prevAvgGain = prevAvgGain.divide(BigDecimal.valueOf(period), 4, BigDecimal.ROUND_HALF_UP);
                prevAvgLoss = prevAvgLoss.divide(BigDecimal.valueOf(period), 4, BigDecimal.ROUND_HALF_UP);
            } else {
                // 后续使用平滑平均
                prevAvgGain = prevAvgGain.multiply(BigDecimal.valueOf(period - 1))
                    .add(gain).divide(BigDecimal.valueOf(period), 4, BigDecimal.ROUND_HALF_UP);
                prevAvgLoss = prevAvgLoss.multiply(BigDecimal.valueOf(period - 1))
                    .add(loss).divide(BigDecimal.valueOf(period), 4, BigDecimal.ROUND_HALF_UP);
            }

            // 计算RSI
            if (prevAvgLoss.compareTo(BigDecimal.ZERO) == 0) {
                rsiValues[i] = BigDecimal.valueOf(100);
            } else {
                BigDecimal rs = prevAvgGain.divide(prevAvgLoss, 4, BigDecimal.ROUND_HALF_UP);
                rsiValues[i] = BigDecimal.valueOf(100)
                    .subtract(BigDecimal.valueOf(100)
                        .divide(BigDecimal.ONE.add(rs), 2, BigDecimal.ROUND_HALF_UP));
            }
        }

        return rsiValues;
    }

    /**
     * 计算布林带指标
     *
     * @param dailyList 日线数据列表
     * @param period 周期（默认20）
     * @param stdDevTimes 标准差倍数（默认2）
     * @return BOLLResult 包含上轨、中轨、下轨
     */
    public BOLLResult calculateBOLL(List<StockDaily> dailyList, int period, int stdDevTimes) {
        if (dailyList == null || dailyList.size() < period) {
            return new BOLLResult();
        }

        int size = dailyList.size();
        BigDecimal[] upper = new BigDecimal[size];
        BigDecimal[] middle = new BigDecimal[size];
        BigDecimal[] lower = new BigDecimal[size];

        BigDecimal[] maValues = calculateMA(dailyList, period);

        for (int i = 0; i < size; i++) {
            if (maValues[i] == null) {
                upper[i] = null;
                middle[i] = null;
                lower[i] = null;
                continue;
            }

            middle[i] = maValues[i];

            // 计算标准差
            BigDecimal sumSquares = BigDecimal.ZERO;
            for (int j = i - period + 1; j <= i; j++) {
                BigDecimal diff = dailyList.get(j).getClose().subtract(middle[i]);
                sumSquares = sumSquares.add(diff.multiply(diff));
            }
            BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(
                sumSquares.divide(BigDecimal.valueOf(period), 6, BigDecimal.ROUND_HALF_UP).doubleValue()
            ));

            upper[i] = middle[i].add(stdDev.multiply(BigDecimal.valueOf(stdDevTimes)))
                .setScale(3, BigDecimal.ROUND_HALF_UP);
            lower[i] = middle[i].subtract(stdDev.multiply(BigDecimal.valueOf(stdDevTimes)))
                .setScale(3, BigDecimal.ROUND_HALF_UP);
        }

        return new BOLLResult(upper, middle, lower);
    }

    // ========== 结果类 ==========

    /**
     * MACD计算结果
     */
    public static class MACDResult {
        private final BigDecimal[] dif;
        private final BigDecimal[] dea;
        private final BigDecimal[] macd;

        public MACDResult() {
            this.dif = new BigDecimal[0];
            this.dea = new BigDecimal[0];
            this.macd = new BigDecimal[0];
        }

        public MACDResult(BigDecimal[] dif, BigDecimal[] dea, BigDecimal[] macd) {
            this.dif = dif;
            this.dea = dea;
            this.macd = macd;
        }

        public BigDecimal[] getDif() { return dif; }
        public BigDecimal[] getDea() { return dea; }
        public BigDecimal[] getMacd() { return macd; }
    }

    /**
     * KDJ计算结果
     */
    public static class KDJResult {
        private final BigDecimal[] k;
        private final BigDecimal[] d;
        private final BigDecimal[] j;

        public KDJResult() {
            this.k = new BigDecimal[0];
            this.d = new BigDecimal[0];
            this.j = new BigDecimal[0];
        }

        public KDJResult(BigDecimal[] k, BigDecimal[] d, BigDecimal[] j) {
            this.k = k;
            this.d = d;
            this.j = j;
        }

        public BigDecimal[] getK() { return k; }
        public BigDecimal[] getD() { return d; }
        public BigDecimal[] getJ() { return j; }
    }

    /**
     * 布林带计算结果
     */
    public static class BOLLResult {
        private final BigDecimal[] upper;
        private final BigDecimal[] middle;
        private final BigDecimal[] lower;

        public BOLLResult() {
            this.upper = new BigDecimal[0];
            this.middle = new BigDecimal[0];
            this.lower = new BigDecimal[0];
        }

        public BOLLResult(BigDecimal[] upper, BigDecimal[] middle, BigDecimal[] lower) {
            this.upper = upper;
            this.middle = middle;
            this.lower = lower;
        }

        public BigDecimal[] getUpper() { return upper; }
        public BigDecimal[] getMiddle() { return middle; }
        public BigDecimal[] getLower() { return lower; }
    }
}