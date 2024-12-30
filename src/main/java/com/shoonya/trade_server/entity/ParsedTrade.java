package com.shoonya.trade_server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name="parsed_trade")
public class ParsedTrade {

    @Column(name="exch")
    private String exch;

    @Column(name="trading_symbol")
    private String tradingSymbol;

    @Id
    @Column(name="buy_time" , nullable = false, updatable = false)
    private LocalDateTime buyTime;

    @Column(name="sell_time")
    private LocalDateTime sellTime;

    @Column(name="qty")
    private int qty;

    @Column(name="buy_price")
    private double buyPrice;

    @Column(name="sell_price")
    private double sellPrice;

    @Column(name="points")
    private double points;

    @Column(name="max_points")
    private double maxPoints;

    public ParsedTrade(){};

    public ParsedTrade(String exch, String tradingSymbol, LocalDateTime buyTime, LocalDateTime sellTime, int qty, double buyPrice, double sellPrice, double points, double maxPoints) {
        this.exch = exch;
        this.tradingSymbol = tradingSymbol;
        this.buyTime = buyTime;
        this.sellTime = sellTime;
        this.qty = qty;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.points = points;
        this.maxPoints = maxPoints;
    }
}
