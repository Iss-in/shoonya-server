package com.shoonya.trade_server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.id.IncrementGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name="trade")
//@Table(name="trade", uniqueConstraints = @UniqueConstraint(columnNames = {"timestamp", "orderType"}))
@Getter
@Setter
public class Trade {

    @Id
    @Column(name="order_id", nullable = false, updatable = false)
    private String orderId;

    @Column(name="timestamp")
    private LocalDateTime timestamp;

    @Column(name="trading_symbol")
    private String tradingSymbol;

    @Column(name="exch")
    private String exch;

    @Column(name="qty")
    private int qty;

    @Column(name="order_type")
    private String orderType;

    @Column(name="price")
    private double price;

    public Trade (){}

    public Trade(String exch, String orderId, LocalDateTime timestamp, String tradingSymbol, int qty, String orderType, double price) {
        this.exch = exch;
        this.orderId = orderId;
        this.timestamp = timestamp;
        this.tradingSymbol = tradingSymbol;
        this.qty = qty;
        this.orderType = orderType;
        this.price = price;
    }
}
