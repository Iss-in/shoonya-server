package com.shoonya.trade_server.repositories;

import com.ibm.icu.impl.ICUNotifier;
import com.shoonya.trade_server.entity.NseSymbols;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NseSymbolsRepository extends JpaRepository<NseSymbols, Integer> {
}
