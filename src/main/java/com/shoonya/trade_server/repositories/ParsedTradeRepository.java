package com.shoonya.trade_server.repositories;

import com.shoonya.trade_server.entity.ParsedTrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ParsedTradeRepository extends JpaRepository<ParsedTrade, LocalDateTime> {
}
