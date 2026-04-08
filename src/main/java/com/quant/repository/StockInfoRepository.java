package com.quant.repository;

import com.quant.entity.StockInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockInfoRepository extends JpaRepository<StockInfo, Long> {

    Optional<StockInfo> findByStockCode(String stockCode);

    boolean existsByStockCode(String stockCode);

    List<StockInfo> findByMarket(String market);

    List<StockInfo> findByIndustry(String industry);

    @Query("SELECT s.stockCode FROM StockInfo s")
    List<String> findAllStockCodes();

    @Query(value = "SELECT COUNT(*) FROM stock_info", nativeQuery = true)
    long countTotalStocks();
}