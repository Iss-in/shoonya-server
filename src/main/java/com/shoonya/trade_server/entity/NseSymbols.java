package com.shoonya.trade_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name="nse_symbols")
public class NseSymbols {

    @Id
    @Column(name="token")
    private Integer token;

    @Column(name="lot_size")
    private int LotSize;

    @Column(name="symbol")
    private String symbol;

    @Column(name="trading_symbol")
    private String tradingSymbol;

    @Column(name="instrument")
    private String instrument;

    @Column(name="tick_size")
    private double tickSize;

}

