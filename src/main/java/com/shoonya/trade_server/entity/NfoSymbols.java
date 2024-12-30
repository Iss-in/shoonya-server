package com.shoonya.trade_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name="nfo_symbols")
public class NfoSymbols {

    @Id
    @Column(name="token")
    private Integer token;

    @Column(name="lot_size")
    private int LotSize;

    @Column(name="symbol")
    private String symbol;

    @Column(name="trading_symbol")
    private String tradingSymbol;

    @Column(name="expiry")
    private LocalDate expiry;

    @Column(name="instrument")
    private String instrument;

    @Column(name="option_type")
    private String optionType;

    @Column(name="strike_price")
    private double strikePrice;

    @Column(name="tick_size")
    private double tickSize;
}
