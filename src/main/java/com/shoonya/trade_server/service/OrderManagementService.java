package com.shoonya.trade_server.service;

import com.shoonya.trade_server.config.IntradayConfig;
import com.shoonya.trade_server.controller.OrderController;
import com.shoonya.trade_server.lib.ShoonyaHelper;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Service
public class OrderManagementService {

    ShoonyaHelper shoonyaHelper;
    TradeManagementService tradeManagementService;
    WebSocketService webSocketService;

    private Logger logger = LoggerFactory.getLogger(OrderManagementService.class.getName());

    public OrderManagementService(ShoonyaHelper shoonyaHelper, TradeManagementService tradeManagementService,
                                  WebSocketService webSocketService, IntradayConfig intradayConfig) {
        this.shoonyaHelper = shoonyaHelper;
        this.tradeManagementService = tradeManagementService;
        this.webSocketService = webSocketService;
    }

    public void modifyOrder(String norenordno, double newPrice) {
        JSONArray orderBook = shoonyaHelper.getOrderBook();

        for (int i = 0; i < orderBook.length(); i++) {
            JSONObject order = orderBook.getJSONObject(i);
            if (order.getString("norenordno").equals(norenordno)) {

                if (order.getString("prctyp").equals("LMT"))
                    shoonyaHelper.modifyOrder(order.getString("exch"), order.getString("tsym"), norenordno,
                            order.getInt("qty"), "LMT", newPrice, 0.0);

                if (order.getString("prctyp").equals("SL-LMT"))
                    shoonyaHelper.modifyOrder(order.getString("exch"), order.getString("tsym"), norenordno,
                            order.getInt("qty"), "SL-LMT", newPrice, newPrice - 0.2);

                break;
            }
        }
    }
    // TODO: what return type should be in service and controller respectively
    public JSONObject buyOrder(String symbol, String priceType, double price) {
        logger.info("buy symbol {} at price {}", symbol, price);
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime lastTradeTime = tradeManagementService.getLastbuyTime();
        long minutesPassed = Duration.between(lastTradeTime, currentTime ).toMinutes();
        long timeRemaining = 15 - minutesPassed;

        if (timeRemaining > 0) {
            webSocketService.sendToast("Buy order rejected", "Wait for timer to end");
            return new JSONObject();
        }
        // place market order if price is 0
        double triggerPrice = 0.0;
        if(priceType.equals("SL-LMT"))
            triggerPrice = price - 0.2;

        int buyQty = tradeManagementService.getBuyQty();
        JSONObject res = shoonyaHelper.placeOrder("B", "M", "NFO", symbol, buyQty, priceType
                , price, triggerPrice);
        logger.info(" manual buy order status:", res.toString()); // TODO: why the response is null
        return res;
//        TODO: pass on correct response to response entity

    }
}