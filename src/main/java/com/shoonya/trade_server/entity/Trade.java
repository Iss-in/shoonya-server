package com.shoonya.trade_server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name="trade")
@Getter
@Setter
public class Trade {

    @Id
    @Column(name="timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name="trading_symbol")
    private String tradingSymbol;

    @Column(name="qty")
    private int qty;

    @Column(name="order_type")
    private String orderType;

    @Column(name="price")
    private double price;

    public Trade (){}

    public Trade(LocalDateTime timestamp, String tradingSymbol, int qty, String orderType, double price) {
        this.timestamp = timestamp;
        this.tradingSymbol = tradingSymbol;
        this.qty = qty;
        this.orderType = orderType;
        this.price = price;
    }
}
