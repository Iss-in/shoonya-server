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

}
