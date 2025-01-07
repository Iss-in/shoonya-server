package com.shoonya.trade_server.service;


import com.shoonya.trade_server.repositories.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AtrTrailingStopService {

    private static final int ATR_PERIOD = 14;
    private static final double ATR_MULTIPLIER = 3.0;
    private Logger logger = LoggerFactory.getLogger(AtrTrailingStopService.class.getName());

    private double atr;
    private double stopLoss;
    private double highestPrice;
    private final List<Double> trList = new ArrayList<>();

    public void updatePrice(double high, double low, double close, double previousClose) {
        double tr = calculateTrueRange(high, low, close, previousClose);
        trList.add(tr);

        if (trList.size() > ATR_PERIOD) {
            trList.remove(0);
        }

        atr = trList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        if (highestPrice == 0.0) {
            highestPrice = close;
            stopLoss = highestPrice - ATR_MULTIPLIER * atr;
        } else if (close > highestPrice) {
            highestPrice = close;
            stopLoss = highestPrice - ATR_MULTIPLIER * atr;
        }
        logger.info("candle low is {} sl is {}", low, stopLoss);
    }

    private double calculateTrueRange(double high, double low, double close, double previousClose) {
        double hl = high - low;
        double hc = Math.abs(high - previousClose);
        double lc = Math.abs(low - previousClose);
        return Math.max(hl, Math.max(hc, lc));
    }

    public double getStopLoss() {
        return stopLoss;
    }

    public boolean checkExit(double currentPrice) {
        return currentPrice <= stopLoss;
    }
}