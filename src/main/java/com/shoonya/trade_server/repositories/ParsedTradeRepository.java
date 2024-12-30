package com.shoonya.trade_server.repositories;

import com.shoonya.trade_server.entity.ParsedTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ParsedTradeRepository extends JpaRepository<ParsedTrade, LocalDateTime> {
    @Query("SELECT COUNT(e) FROM ParsedTrade e WHERE e.points > :value AND DATE(e.buyTime) = :date ")
    int countByColumnValueExceeds(@Param("value") int value, @Param("date") LocalDate date);

    @Query("SELECT e FROM ParsedTrade e WHERE DATE(e.buyTime) = :date ")
    List<ParsedTrade> getTradesByDate( @Param("date") LocalDate date);

}
