package com.shoonya.trade_server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "daily_record")
public class DailyRecord {

    @Id
    @Column(name="date")
    private LocalDate date;

    @Column(name="Trades")
    private Integer trades;

    @Column(name="successful_trades")
    private Integer successfulTrades ;

    @Column(name="account_value")
    private Integer accountValue ;

    @Column(name="peak_value")
    private Integer peakValue ;

    @Column(name="drawdown")
    private Integer drawdown ;

    @Column(name="pnl")
    private Integer pnl ;

    @Column(name="max_loss")
    private Integer maxLoss ;

    public DailyRecord(){

    }

    public DailyRecord(LocalDate date, Integer trades, Integer successfulTrades, Integer accountValue, Integer peakValue, Integer drawdown, Integer pnl) {
        this.date = date;
        this.trades = trades;
        this.successfulTrades = successfulTrades;
        this.accountValue = accountValue;
        this.peakValue = peakValue;
        this.drawdown = drawdown;
        this.pnl = pnl;
    }
}
