package com.shoonya.trade_server.entity;

import java.time.Instant;

class Candlestick {
    Instant startTime;
    double open;
    double high;
    double low;
    double close;

    public Candlestick(Instant startTime, double open, double high, double low, double close) {
        this.startTime = startTime;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
    }

}