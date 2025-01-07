package com.shoonya.trade_server.service;

import com.shoonya.trade_server.controller.MoneyController;
import com.shoonya.trade_server.entity.TokenInfo;
import com.shoonya.trade_server.handler.WebSocketHandler;
import com.shoonya.trade_server.lib.Misc;
import com.shoonya.trade_server.lib.ShoonyaHelper;
import lombok.Getter;
import lombok.Setter;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Service
public class OptionUpdateService {

    private TradeManagementService tradeManagementService;
    private WebSocketService webSocketService;
    private ShoonyaHelper shoonyaHelper;
    private Misc misc;
    LocalDate expiry;

    public OptionUpdateService(TradeManagementService tradeManagementService, Misc misc, WebSocketService webSocketService
    , ShoonyaHelper shoonyaHelper){
        this.tradeManagementService = tradeManagementService;
        this.expiry = misc.getNseExpiry();
        this.webSocketService = webSocketService;
        this.misc = misc;
        this.shoonyaHelper = shoonyaHelper;
    }

    private static final Logger logger = LogManager.getLogger(OptionUpdateService.class.getName());
//    LocalDate expiry = this.misc.getNseExpiry();
    private String ceTsym , ceToken, peTsym, peToken;
    private int atmCe = 0, atmPe = 0;

    @Scheduled(fixedRate = 2000)
    public void pollLatestOptions() throws InterruptedException {
        setLastestOptions(false);
    }

    public void setLastestOptions(boolean reset) throws InterruptedException {

        if(reset) {
            this.atmCe = 0;
            this.atmPe = 0;
        }

        Map<String, Double> ltps = tradeManagementService.getLtps();
        int indexPrice = (int) Math.round(ltps.get("26000"));

        // Adjust index price to the nearest multiple of 50
        int roundedIndexPrice = (indexPrice + 25) / 50 * 50;

        int[] newPrices = getClosestATMOptions(roundedIndexPrice);

        boolean flag = false;

        if(Math.abs(indexPrice - this.atmCe) > 30 &&  this.atmCe != newPrices[0] ) {
            logger.info("atm ce strike changed from {} to {}",  this.atmCe , newPrices[0]);
            this.atmCe = newPrices[0];
            flag = true;
        }

        if(Math.abs(indexPrice - this.atmPe) > 30 &&  this.atmPe != newPrices[1] ) {
            logger.info("atm pe strike changed from {} to {}",  this.atmPe ,  newPrices[1]);
            this.atmPe = newPrices[1];
            flag = true;
        }

        if(flag) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMMyy");
            String formattedDate = expiry.format(formatter).toUpperCase(); // Ensure the month is in uppercase

            String ceTsym = "NIFTY" + formattedDate + 'C' + atmCe;
            String ceToken = misc.getToken("NFO", ceTsym);
            String peTsym = "NIFTY" + formattedDate + 'P' + atmPe;
            String peToken = misc.getToken("NFO", peTsym);

            webSocketService.updateAtmOptions(ceToken, ceTsym, peToken, peTsym );
            tradeManagementService.unsubscribe(new TokenInfo("NFO", ceToken, ceTsym));
            tradeManagementService.unsubscribe(new TokenInfo("NFO", peToken, peTsym));
            tradeManagementService.subscribe(new TokenInfo("NFO", ceToken, ceTsym));
            tradeManagementService.subscribe(new TokenInfo("NFO", peToken, peTsym));
            webSocketService.updateOrderFeed(tradeManagementService.getOpenOrders());
            webSocketService.updatePositionFeed(shoonyaHelper.getPositions());

        }
    }

    // Function to generate option strike prices based on index price
    public static List<Integer> generateStrikePrices(int indexPrice, int step, int rangeCount) {
        List<Integer> strikes = new ArrayList<>();
        for (int i = -rangeCount; i <= rangeCount; i++) {
            strikes.add((indexPrice + i * step) / step * step);
        }
        return strikes;
    }

    // Function to find closest ATM options
    public static int[] getClosestATMOptions(int indexPrice) {
        List<Integer> strikes = generateStrikePrices(indexPrice, 50, 5);

        int closestCall = 0;
        int closestPut = 0;
        int closestCallDiff = Integer.MAX_VALUE;
        int closestPutDiff = Integer.MAX_VALUE;

        for (int strike : strikes) {
            int diff = Math.abs(indexPrice - strike);

            if (strike >= indexPrice && diff < closestCallDiff) {
                closestCallDiff = diff;
                closestCall = strike;
            } else if (strike <= indexPrice && diff < closestPutDiff) {
                closestPutDiff = diff;
                closestPut = strike;
            }
        }

        return new int[]{closestCall, closestPut};
    }

}
