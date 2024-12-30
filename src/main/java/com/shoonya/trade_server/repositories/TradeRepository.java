package com.shoonya.trade_server.repositories;

import com.shoonya.trade_server.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, LocalDateTime> {
    @Query(value = "SELECT * FROM trade " +
            "WHERE timestamp BETWEEN date_trunc('day', now()) " +
            "AND date_trunc('day', now()) + interval '1 day' - interval '1 second' " +
            "ORDER BY timestamp ASC",
            nativeQuery = true)
    List<Trade> findTodayTrades();

    @Query("SELECT o FROM Trade o WHERE o.timestamp BETWEEN :startOfDay AND :endOfDay ORDER BY o.timestamp ASC")
    List<Trade> findTradesByDate(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT tr from Trade tr WHERE tr.timestamp = :timestamp and tr.orderType = :orderType")
    Trade getByTimestampOrder(@Param("timestamp") LocalDateTime timestamp, @Param("orderType") String orderType);

}
