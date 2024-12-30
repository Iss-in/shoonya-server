package com.shoonya.trade_server.service;

import com.shoonya.trade_server.controller.MoneyController;
import com.shoonya.trade_server.entity.TokenInfo;
import com.shoonya.trade_server.lib.Misc;
import lombok.Getter;
import lombok.Setter;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
    private int atmCe = 0;
    private int atmPe = 0;
    private Misc misc;

    public OptionUpdateService(TradeManagementService tradeManagementService, Misc misc ){
        this.tradeManagementService = tradeManagementService;
        this.misc = misc;
    }

    private static final Logger logger = LogManager.getLogger(OptionUpdateService.class.getName());

    @Scheduled(fixedRate = 2000)
    public void setLastestOptions(){
        Map<String, Double> ltps = tradeManagementService.getLtps();
        int indexPrice = (int) Math.round(ltps.get("26000"));

        // Adjust index price to the nearest multiple of 50
        int roundedIndexPrice = (indexPrice + 25) / 50 * 50;

        int[] newPrices = getClosestATMOptions(roundedIndexPrice);

        if(Math.abs(indexPrice - atmCe) > 30 &&  this.atmCe != newPrices[0] ) {
            logger.info("atm ce strike changed from {} to {}",  this.atmCe , newPrices[0]);
            this.atmCe = newPrices[0];
        }

        if(Math.abs(indexPrice - atmPe) > 30 &&  this.atmPe != newPrices[1] ) {
            logger.info("atm pe strike changed from {} to {}",  this.atmPe ,  newPrices[1]);
            this.atmPe = newPrices[1];
        }
    }

    public JSONObject getAtmSymbols(){
        JSONObject ret = new JSONObject();
        LocalDate expiry = misc.getNseWeeklyExpiry("NIFTY", 0);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMMyy");
        String formattedDate = expiry.format(formatter).toUpperCase(); // Ensure the month is in uppercase

        String ceTsym = "NIFTY" + formattedDate +'C' + atmCe;
        String CeToken = misc.getToken("NFO", ceTsym);
        String peTsym = "NIFTY" + formattedDate +'P' + atmPe;
        String PeToken = misc.getToken("NFO", peTsym);

        tradeManagementService.subscribe(new TokenInfo("NFO", CeToken,null));
        tradeManagementService.subscribe(new TokenInfo("NFO", PeToken,null));

        ret.put("atmCall",ceTsym);
        ret.put("atmPut",peTsym);

        return ret;
    }

    public JSONObject getAtmPrice(String str){
        JSONObject ret = new JSONObject();
        String[] symbols = str.split(",");
        String CeToken = misc.getToken("NFO", symbols[0]);
        String PeToken = misc.getToken("NFO", symbols[1]);

        Map<String, Double> ltps = tradeManagementService.getLtps();

        ret.put(symbols[0],ltps.get(CeToken));
        ret.put(symbols[1],ltps.get(PeToken));

        return ret;
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
