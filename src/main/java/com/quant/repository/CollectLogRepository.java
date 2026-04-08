package com.quant.repository;

import com.quant.entity.CollectLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CollectLogRepository extends JpaRepository<CollectLog, Long> {

    List<CollectLog> findByCollectTypeOrderByStartTimeDesc(String collectType);

    List<CollectLog> findByStockCodeOrderByStartTimeDesc(String stockCode);

    List<CollectLog> findByStatus(String status);

    @Query("SELECT c FROM CollectLog c WHERE c.startTime >= :startTime ORDER BY c.startTime DESC")
    List<CollectLog> findAfterStartTime(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT COUNT(c) FROM CollectLog c WHERE c.collectType = :type AND c.status = :status")
    long countByTypeAndStatus(@Param("type") String type, @Param("status") String status);

    @Query(value = "SELECT stock_code FROM collect_log WHERE collect_type='daily' AND status='success' GROUP BY stock_code",
           nativeQuery = true)
    List<String> findSuccessfullyCollectedStockCodes();
}