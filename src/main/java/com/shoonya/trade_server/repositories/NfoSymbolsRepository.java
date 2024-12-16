package com.shoonya.trade_server.repositories;

import com.shoonya.trade_server.entity.NfoSymbols;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;

public interface NfoSymbolsRepository extends JpaRepository<NfoSymbols, Integer> {
}
