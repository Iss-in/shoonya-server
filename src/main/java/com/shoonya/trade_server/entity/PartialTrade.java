package com.shoonya.trade_server.entity;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartialTrade {
    private String name;

    // 0:inactive, 1:active, 2:completed
    private int status;

    private int qty;

    private Double entryPrice;

    private Double exitPrice;

    private Double slPrice;

    private Double maxSlPrice;

    private Double targetPrice;

    private String orderNumber;

    private String orderType;

    private String prd;

    private String exch;

    private String tsym;

    private Double diff;

    private String token;

    public PartialTrade(String name, int status, int qty, Double entryPrice, Double slPrice,
                        Double maxSlPrice, Double targetPrice, String orderType, String prd,
                        String exch, String tsym,  Double diff, String token) {
        this.name = name;
        this.status = status;
        this.qty = qty;
        this.entryPrice = entryPrice;
        this.slPrice = slPrice;
        this.targetPrice = targetPrice;
        this.maxSlPrice = maxSlPrice;
        this.orderType = orderType;
        this.prd = prd;
        this.exch = exch;
        this.tsym = tsym;
        this.diff = diff;
        this.token = token;

    }
}
