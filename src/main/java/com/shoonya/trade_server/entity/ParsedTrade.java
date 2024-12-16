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

    @Id
    @Column(name="buy_time" , nullable = false, updatable = false)
    private LocalDateTime buyTime;

    @Column(name="sell_time")
    private LocalDateTime sellTime;

    @Column(name="qty")
    private int qty;

    @Column(name="points")
    private double points;

    public ParsedTrade(){};

    public ParsedTrade(LocalDateTime buyTime, LocalDateTime sellTime, int qty, double points) {
        this.buyTime = buyTime;
        this.sellTime = sellTime;
        this.qty = qty;
        this.points = points;
    }
}
