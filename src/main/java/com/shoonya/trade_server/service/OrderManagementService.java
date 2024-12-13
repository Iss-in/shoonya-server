package com.shoonya.trade_server.service;
import com.shoonya.trade_server.lib.ShoonyaHelper;
import com.noren.javaapi.NorenApiJava;
import com.shoonya.trade_server.entity.PartialTrade;
import com.shoonya.trade_server.entity.TokenInfo;
import com.shoonya.trade_server.lib.Misc;
import com.shoonya.trade_server.lib.ShoonyaWebSocket;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.max;

@Getter
@Setter
class TradeManager{

    private final Map<String, Map<String, PartialTrade>> trades;
    public TradeManager() {
        trades = new HashMap<>();
    }
    public void addTrade(String token, String tradeName, PartialTrade trade) {
        trades.computeIfAbsent(token, k -> new HashMap<>()).put(tradeName, trade);
    }

    // Method to retrieve a trade
    public Map<String, PartialTrade> getTrade(String token) {
        return trades.getOrDefault(token, new HashMap<>());
    }

    public boolean removeTrade(String token){
        Map<String, PartialTrade> trade = trades.get(token);
        if(trade != null)
            return trades.remove(token, trade);
        return false;
    }

    public void updateTrade(String token, String pt, PartialTrade partialTrade){
        trades.get(token).put(pt, partialTrade);
    }

    public boolean hasToken(String token){
        return trades.containsKey(token);
    }
}


@Service
public class OrderManagementService {

    private static final Logger logger = LogManager.getLogger(OrderManagementService.class);

    TradeManager tradeManager;

    Misc misc;

    ShoonyaHelper shoonyaHelper;

    public OrderManagementService(ShoonyaHelper shoonyaHelper, Misc misc){
        this.shoonyaHelper = shoonyaHelper;
        this.tradeManager = new TradeManager();
        this.misc = misc;
    }

    public void createTrade(String token, JSONObject orderUpdate){

        int qty = (Integer.parseInt( orderUpdate.getString("fillshares")));

        Double entryPrice = Double.parseDouble(orderUpdate.getString("flprc"));
        String exch = orderUpdate.getString("exch");
        String tsym = orderUpdate.getString("tsym");
        String pcode = orderUpdate.getString("pcode");


        Double slPrice = entryPrice - misc.getMaxSl(exch, token) / 2;
        slPrice = max(slPrice, 0.1);

        Double maxSlprice = entryPrice - misc.getMaxFutSl(exch, token)/2;
        maxSlprice = max(maxSlprice, 0.1);

        Double diff = misc.getTriggerdiff(exch, token );
        int minLotSize = misc.getMinLotSize(exch, token );
        List<Double> targets = misc.getTargets(exch, token );


        int div = 2;

        int multiple = qty/(div * minLotSize);
        int remaining = qty - div * minLotSize * multiple;
        PartialTrade trade;

        if(multiple > 0) {
            logger.info("qty {} more than 3x min quantity {}", qty, 3 * minLotSize);

            for (int i = div; i > 0; i--) {
                String tradeName = "t" + i;
                int tradeQty = minLotSize * multiple;
                if (remaining > 0) {
                    tradeQty += minLotSize;
                    remaining -= minLotSize;
                }
                // TODO change diff per volatility

                logger.info("for {}, using qty {}", tradeName, tradeQty);
                trade = new PartialTrade(tradeName, 0, tradeQty, entryPrice,
                        slPrice, maxSlprice,entryPrice + targets.get(i - 1) , "SL-LMT",
                        pcode, exch, tsym, diff);

                this.tradeManager.addTrade(token, tradeName, trade);
            }
        }

        else if(remaining > 0){
            logger.info("qty {} less than div x min quantity {}", qty,  div * minLotSize);
            multiple = qty / minLotSize;
            for (int j=1; j < multiple+1; j++){
                String tradeName = "t" + j;
                logger.info("for {}, using qty {}", tradeName, minLotSize);

                trade = new PartialTrade(tradeName, 0, minLotSize, entryPrice, slPrice, maxSlprice, entryPrice + targets.get(j - 1), "SL-LMT",
                        pcode, exch, tsym, diff);
                this.tradeManager.addTrade(token, tradeName, trade);
            }
        }
    }

    public void handleBuyOrder(ShoonyaWebSocket wsClient, String token, String exch, JSONObject orderUpdate){

        if( (orderUpdate.getString("trantype").equals("B") && orderUpdate.getString("status").equals("COMPLETE")) ||
                (orderUpdate.getString("trantype").equals("B")  && orderUpdate.getString("status").equals("CANCELED") && orderUpdate.has("fillshares"))){
            if(!tradeManager.hasToken(token) ){
                createTrade(token, orderUpdate);
                subscribe(wsClient, new TokenInfo(exch, token,null));
            }
        }
    }

    public void handleSellOrder(ShoonyaWebSocket wsClient, String token, String exch, JSONObject orderUpdate){
        if( orderUpdate.getString("trantype").equals("S") && orderUpdate.getString("status").equals("COMPLETE")){
            logger.info("selling order is {}", orderUpdate);

            Map<String, PartialTrade> trades = tradeManager.getTrade(token);

//            for(String key:trades.keySet()){
//                PartialTrade partialTrade = trades.get(key);

            for(Map.Entry<String, PartialTrade> entry: trades.entrySet()){
                String pt = entry.getKey();
                PartialTrade partialTrade = entry.getValue();
                if(partialTrade.getOrderNumber().equals(orderUpdate.getString("norenordno"))) {
                    partialTrade.setExitPrice(Double.parseDouble(orderUpdate.getString("flprc")));
                    partialTrade.setStatus(2);
                    tradeManager.updateTrade(token, pt, partialTrade);
                    logger.info("{} completed {}", pt, partialTrade);
                }
            }


            Boolean flag = true;
            for(Map.Entry<String, PartialTrade> entry: trades.entrySet()){
                PartialTrade partialTrade = entry.getValue();
                if(partialTrade.getStatus() != 2){
                    flag = false;
                    break;
                }
            }

            if(flag){
                unsubscribe(wsClient, new TokenInfo(exch, token, null));
                logger.info("all active trades for token {} completed", token);
                logger.info("unsubscribed for token {}", token);
                boolean status = tradeManager.removeTrade(token);
                logger.debug("token {} removed from all trades with status {}", token, status);
                logger.info("All trades completed, final Trade is \n {} ",tradeManager.getTrades() );

            }
        }
    }

    public void updateOrder( ShoonyaWebSocket wsClient, @NotNull JSONObject orderUpdate){

        String exch = orderUpdate.getString("exch");
        String tsym = orderUpdate.getString("tsym");
        String token = misc.getToken(exch, tsym);

        if (orderUpdate.has("rejreason")){
            logger.info("order rejected {}", orderUpdate);
            return;
        }

        handleBuyOrder(wsClient, token, exch, orderUpdate);
        handleSellOrder(wsClient, token, exch, orderUpdate);
    }

    // TODO: figure out threading in this place
    public void placeSl(String pt, String token, PartialTrade trade){
        if(trade.getStatus() > 0)
            return ;
        logger.info("placing sl for a fresh order for {} ", trade);
        logger.info("placing sl for a fresh order for {} ", trade.getName());
        JSONObject res = this.shoonyaHelper.placeOrder("S", trade.getPrd(), trade.getExch(), trade.getTsym(),
                trade.getQty(), "SL-LMT", trade.getSlPrice(), trade.getSlPrice() + trade.getDiff() );


        String orderNumber = res.getString("norenordno");
        if(!res.has("resreason")) {
            trade.setOrderNumber(orderNumber);
            trade.setStatus(1);
            tradeManager.updateTrade(token, pt, trade);
        }
    }

    public void manageTrade(Double ltp, String token, String pt, PartialTrade trade){

        Double points = ltp - trade.getEntryPrice();
        JSONObject ret;

        if(points >= 2.0f/3 * trade.getTargetPrice() && trade.getOrderType() .equals("SL-LMT")){
            logger.info("modifying sl order from SL-LMT to LIMIT");
            ret = this.shoonyaHelper.modifyOrder(trade.getExch(), trade.getTsym(), trade.getOrderNumber() ,trade.getQty()
                    ,"LMT", trade.getTargetPrice(), null);
            trade.setOrderType("LMT");
            logger.info("sl order modified from SL-LMT to LMT with target {}", trade.getTargetPrice());
            logger.info(ret);
        }
        if(points <= 1.0f/3 * trade.getTargetPrice() && trade.getOrderType().equals("LMT")){
            logger.info("modifying target order from LIMIT to SL-LMT ");

            ret = this.shoonyaHelper.modifyOrder(trade.getExch(), trade.getTsym(), trade.getOrderNumber() ,trade.getQty() ,
                    "SL-LMT", trade.getSlPrice(),trade.getSlPrice() + trade.getDiff() );
            trade.setOrderType("SL-LMT");
            logger.info("sl order modified from LIMIT to SL-LMT with sl {}", trade.getSlPrice());
            logger.info(ret);
        }
        if(ltp < trade.getMaxSlPrice()){
            logger.info("limit sl order crossed, exiting all trades with market orders");
            this.shoonyaHelper.exitAllMarketOrders();
//            #TODO: make market order exit function
        }
        tradeManager.updateTrade(token, pt, trade);
    }


    public void manageOptionSl(String token, Double ltp ){
        if(!tradeManager.hasToken(token)){
            logger.debug("trade status false or current token is not of current trade");
            return;
        }
        // TODO: make number of threads dyanamic per number of trades ? or keep a high number
        Map<String, PartialTrade> trades = tradeManager.getTrade(token);
        ExecutorService executor = null;

        // TODO: figure out how to stop thread from placing extra orders ? or it is really placing extra orders because of retry code?
        try {
            executor = Executors.newFixedThreadPool( trades.size() );

            for (Map.Entry<String, PartialTrade> entry : trades.entrySet()) {
                String pt = entry.getKey();
                PartialTrade partialTrade = entry.getValue();
                executor.submit(() -> placeSl(pt, token, partialTrade));
            }
        }
        finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); // Force shutdown if not terminated
                }            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }


        // TODO: figure out how to stop thread from placing extra orders ? or it is really placing extra orders because of retry code?
//        for(String pt:trades.keySet()){
//            PartialTrade partialTrade = trades.get(pt);
//            executor.submit(() -> {
//                manageTrade(ltp, pt, partialTrade);
//            });
//        }
        // TODO: add in notes, entrySet is better than keySet in iterating over a map, efficient, as dont have to fetch value everytime

        trades = tradeManager.getTrade(token);

        try {
            executor = Executors.newFixedThreadPool(3);

            for (Map.Entry<String, PartialTrade> entry : trades.entrySet()) {
                String pt = entry.getKey();
                PartialTrade partialTrade = entry.getValue();
                executor.submit(() -> manageTrade(ltp, token, pt, partialTrade));
            }
        }
        finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); // Force shutdown if not terminated
                }            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }



    }


    public void subscribe(ShoonyaWebSocket wsClient, TokenInfo tokenInfo){
        String instrument = tokenInfo.getInstrument();
        wsClient.subscribe(instrument, NorenApiJava.FeedType.TOUCHLINE);
        logger.info("subscribed to {}", instrument );
    }

    public void unsubscribe(ShoonyaWebSocket wsClient, TokenInfo tokenInfo){
        String instrument = tokenInfo.getInstrument();
        wsClient.unsubscribe(instrument, NorenApiJava.FeedType.TOUCHLINE);
        logger.info("unsubscribed from {}", instrument );
    }

}
