package com.shoonya.trade_server.repositories;

import com.shoonya.trade_server.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteRepository extends JpaRepository<Quote, Long> {



    @Query(value = "SELECT * FROM quote WHERE id != :id ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Quote getRandomQuote(Long id);
}
