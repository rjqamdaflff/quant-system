package com.quant.repository;

import com.quant.entity.StockDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockDailyRepository extends JpaRepository<StockDaily, Long> {

    Optional<StockDaily> findByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);

    boolean existsByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);

    List<StockDaily> findByStockCodeOrderByTradeDateDesc(String stockCode);

    List<StockDaily> findByStockCodeAndTradeDateBetween(
        String stockCode, LocalDate startDate, LocalDate endDate);

    @Query("SELECT MAX(d.tradeDate) FROM StockDaily d WHERE d.stockCode = :stockCode")
    Optional<LocalDate> findLatestTradeDateByStockCode(@Param("stockCode") String stockCode);

    @Query("SELECT MIN(d.tradeDate) FROM StockDaily d WHERE d.stockCode = :stockCode")
    Optional<LocalDate> findEarliestTradeDateByStockCode(@Param("stockCode") String stockCode);

    @Query("SELECT COUNT(d) FROM StockDaily d WHERE d.stockCode = :stockCode")
    long countByStockCode(@Param("stockCode") String stockCode);

    @Query(value = "SELECT * FROM stock_daily WHERE stock_code = :stockCode " +
                   "ORDER BY trade_date DESC LIMIT :limit", nativeQuery = true)
    List<StockDaily> findRecentByStockCode(@Param("stockCode") String stockCode, @Param("limit") int limit);

    @Modifying
    @Query(value = "INSERT INTO stock_daily (stock_code, trade_date, open, close, high, low, volume, amount, change_pct, turnover, created_at) " +
                   "VALUES (:stockCode, :tradeDate, :open, :close, :high, :low, :volume, :amount, :changePct, :turnover, NOW()) " +
                   "ON DUPLICATE KEY UPDATE open=:open, close=:close, high=:high, low=:low, volume=:volume, amount=:amount, change_pct=:changePct, turnover=:turnover",
           nativeQuery = true)
    int insertOrUpdateDaily(
        @Param("stockCode") String stockCode,
        @Param("tradeDate") LocalDate tradeDate,
        @Param("open") BigDecimal open,
        @Param("close") BigDecimal close,
        @Param("high") BigDecimal high,
        @Param("low") BigDecimal low,
        @Param("volume") Long volume,
        @Param("amount") BigDecimal amount,
        @Param("changePct") BigDecimal changePct,
        @Param("turnover") BigDecimal turnover
    );

    // Data validation queries
    @Query("SELECT d FROM StockDaily d WHERE d.changePct < -11 OR d.changePct > 11")
    List<StockDaily> findAbnormalChangePct();

    @Query("SELECT d FROM StockDaily d WHERE d.volume < 0")
    List<StockDaily> findNegativeVolume();

    @Query("SELECT d FROM StockDaily d WHERE d.close <= 0 OR d.open <= 0")
    List<StockDaily> findInvalidPrices();
}