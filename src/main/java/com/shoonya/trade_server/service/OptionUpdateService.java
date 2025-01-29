package com.shoonya.trade_server.service;

import com.shoonya.trade_server.entity.SessionVars;
import com.shoonya.trade_server.entity.TokenInfo;
import com.shoonya.trade_server.handler.WebSocketHandler;
import com.shoonya.trade_server.lib.Mibian;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Getter
@Service
public class OptionUpdateService {

    private TradeManagementService tradeManagementService;
    private WebSocketService webSocketService;
    private ShoonyaHelper shoonyaHelper;
    private Misc misc;
    LocalDate expiry;
    Mibian mibian;

    public OptionUpdateService(TradeManagementService tradeManagementService, Misc misc, WebSocketService webSocketService
    , ShoonyaHelper shoonyaHelper, Mibian mibian, SessionVars sessionVars) {
        this.tradeManagementService = tradeManagementService;
//        this.expiry = misc.getNseExpiry();
        this.expiry = sessionVars.getNiftyExpiry();
        this.webSocketService = webSocketService;
        this.misc = misc;
        this.shoonyaHelper = shoonyaHelper;
        this.mibian = mibian;
        updateAtmoptions();

    }

    public void updateAtmoptions()  {
        Map<String, Double> ltps =  tradeManagementService.getLtps();
        try {
            while (!ltps.containsKey("26000")) {
                Thread.sleep(1000);
                logger.info("sleeping waiting for to subscribe nifty token");
            }
        } catch (Exception e){
            logger.error(e.getMessage());
        }
        int latestPrice = (int) Math.round(ltps.get("26000"));
        latestPrice = (latestPrice + 25) / 50 * 50;
        this.atmCe = latestPrice;
        this.atmPe = this.atmCe;
    }


    private static final Logger logger = LogManager.getLogger(OptionUpdateService.class.getName());
//    LocalDate expiry = this.misc.getNseExpiry();
    private String ceTsym , ceToken, peTsym, peToken;
    private int atmCe ,atmPe ;

    @Scheduled(fixedRate = 2000)
    public void pollLatestOptions() throws InterruptedException {
        setLastestOptions(false);
    }

    public void setLastestOptions(boolean reset) throws InterruptedException {



        Map<String, Double> ltps = tradeManagementService.getLtps();
        int indexPrice = (int) Math.round(ltps.get("26000"));

        // Adjust index price to the nearest multiple of 50
        int roundedIndexPrice = (indexPrice + 25) / 50 * 50;

        boolean flag = getClosestATMOptions(roundedIndexPrice);

        // do not update options if trade is active
        if(tradeManagementService.getTradeManager().isTradeActive())
            return;

        if(flag || reset) {
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

    public double getDelta(int indexPrice ,int strikePrice){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryEnd = expiry.atTime(15, 29, 59);
        int days = (int) (Duration.between(now, expiryEnd  ).getSeconds() / 86400);
        Mibian.BS bs = new Mibian.BS(new double[]{indexPrice, strikePrice, 7, days}, 18.0, null, null, null);
        double delta = bs.getCallDelta();
        return delta;
    }

    public List<Double> generateDeltas(int indexPrice, LocalDate expiry, List<Integer> strikes ){
        List<Double> deltas = new ArrayList<>();
        for(int strikePrice:strikes){
            double delta = getDelta(indexPrice, strikePrice);
            deltas.add(delta);
        }

        return deltas;
    }
    // Function to find closest ATM options
    public boolean isWithinThreshold(double currentDelta, double targetDelta, double threshold){
        if(targetDelta - threshold <= currentDelta && currentDelta <= targetDelta + threshold)
            return true;
        return false;
    }

    public boolean getClosestATMOptions(int indexPrice) {
        boolean flag = false;
        List<Integer> strikes = generateStrikePrices(indexPrice, 50, 5);

        List<Double> callDeltas = generateDeltas(indexPrice, this.expiry, strikes);
        List<Double> putDeltas = new ArrayList<>();

        // Calculate putDeltas as 1 - corresponding callDelta
        for (Double callDelta : callDeltas) {
            putDeltas.add(1 - callDelta);
        }

        double currentCallDelta = getDelta(indexPrice, this.atmCe);
        double currentPutDelta = 1 - getDelta(indexPrice, this.atmPe);

        double newCallDelta = 0;int callDeltaIndex = 0;
        for(callDeltaIndex = callDeltas.size() -1 ;callDeltaIndex >= 0; callDeltaIndex--){
            double targetCallDelta = callDeltas.get(callDeltaIndex);
            if(targetCallDelta >= 0.5){
                newCallDelta = targetCallDelta;
                break;
            }
        }
        double newPutDelta = 0; int putDeltaIndex = 0;
        for(putDeltaIndex = 0;putDeltaIndex < putDeltas.size();putDeltaIndex++){
            double targetPutDelta = putDeltas.get(putDeltaIndex);
            if(targetPutDelta >= 0.5){
                newPutDelta = targetPutDelta;
                break;
            }
        }

        if(Math.abs(newCallDelta - currentCallDelta) > 0.05 ||  currentCallDelta < 0.5) {
            this.atmCe = strikes.get(callDeltaIndex);
            flag = true;
        }

        if(Math.abs(newPutDelta - currentPutDelta) > 0.05 ||  currentPutDelta < 0.5) {
            this.atmPe = strikes.get(putDeltaIndex);
            flag = true;
        }
        return flag;
    }

}
