package com.shoonya.trade_server.lib;

import com.noren.javaapi.NorenApiJava;
import com.shoonya.trade_server.service.ShoonyaLoginService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

//TODO: check type of beans and their function
@Component
public class ShoonyaHelper {

    private final NorenApiJava api;
    public ShoonyaHelper(ShoonyaLoginService shoonyaLoginService){
        this.api = shoonyaLoginService.getApi();
    }

    private  static final Logger logger = LoggerFactory.getLogger(ShoonyaHelper.class.getName());

    public JSONArray getTradebook(){

        return this.api.get_trade_book();
    }

    public JSONArray getPositions(){

        return this.api.get_positions();
    }

    public double getBrokerage(){
        JSONObject ret = this.api.get_limits();
        if(ret == null)
            return 0;

        double brokerage = 0;
        if(ret.has("brokerage"))
            brokerage = ret.getFloat("brokerage");
        return brokerage;
    }

    public double getPnl(){
        JSONArray positions = this.api.get_positions();

        if(positions == null)
            return 0;

        double pnl = 0;
        double mtm = 0;

        for (int i = 0; i < positions.length(); i++) {
            JSONObject position = positions.getJSONObject(i);
            pnl += position.optFloat("rpnl", 0);   // Add realized PnL, default to 0 if missing
            mtm += position.optFloat("urmtom", 0); // Add// Add unrealized MTM PnL
        }

        return pnl + mtm - getBrokerage();

    }

    public int getTradeCount() {
        int count = 0;

        JSONArray ret = getTradebook();
        if(ret == null)
            return count;

        List<String>order_uid = new ArrayList<>();
        for(int i=0;i< ret.length();i++) {
            JSONObject order = ret.getJSONObject(i);
            String order_id = order.getString("norenordno");
            if (!order_uid.contains(order_uid)) {
                order_uid.add(order_id);
                if(order.getString("trantype") .equals("B"))
                    count = count + 1;
            }
        }
        return count;
    }



    private static final int MAX_TRIES = 5;

    public JSONObject modifyOrder(String exchange, String tradingsymbol, String orderNumber,
                                                  int quantity, String newPriceType, Double newPrice, Double newTriggerPrice) {
        JSONObject res = new JSONObject();
        res.put("rejreason", true);
        int currentTries = 0;

        try {
            while (res == null || res.has("rejreason")) {
                switch (newPriceType) {
                    case "MKT":
                        logger.debug("Running command api.modifyOrder(exchange={}, tradingsymbol={}, orderNumber={}, " +
                                "newQuantity={}, newPriceType='MKT', newPrice=0.0)", exchange, tradingsymbol, orderNumber, quantity);
                        res = this.api.modify_order(orderNumber, exchange, tradingsymbol, quantity, "MKT", 0.0, null, null, null, null);

                        break;

                    case "SL-LMT":
                        logger.debug("Running command api.modifyOrder(exchange={}, tradingsymbol={}, orderNumber={}, " +
                                        "newQuantity={}, newPriceType='SL-LMT', newPrice={}, newTriggerPrice={})",
                                exchange, tradingsymbol, orderNumber, quantity, newPrice, newTriggerPrice);
                        res = this.api.modify_order(orderNumber, exchange, tradingsymbol, quantity, "SL-LMT", newPrice, newTriggerPrice, null, null, null);
                        break;

                    case "LMT":
                        logger.debug("Running command api.modifyOrder(exchange={}, tradingsymbol={}, orderNumber={}, " +
                                "newQuantity={}, newPriceType='LMT', newPrice={})", exchange, tradingsymbol, orderNumber, quantity, newPrice);
                        res = this.api.modify_order(orderNumber, exchange, tradingsymbol, quantity, "LMT", newPrice, null, null, null, null);
                        break;

                    default:
                        logger.error("Unsupported newPriceType: {}", newPriceType);
                        return res;
                }

                logger.info("Response: {}", res);
                currentTries++;

                if (currentTries >= MAX_TRIES) {
                    logger.error("Max attempts to modify order failed");
                    break;
                }

                Thread.sleep(2000); // sleeping for 2 seconds
            }
        } catch (Exception e) {
            logger.error("Error in modifying order: {}", e.getMessage(), e);
        }

        return res;
    }

    public  JSONObject placeOrder(String orderType, String productType, String exchange,
                                                 String tradingsymbol, int quantity, String newPriceType, double price,
                                                 Double triggerPrice) {
        JSONObject res = new JSONObject();
        res.put("rejreason", true);  // Assuming 'rejreason' should be in the response map initially
        int currentTries = 0;

        try {
            // Process orders based on newPriceType
            while (res.has("rejreason")) {
                switch (newPriceType) {
                    case "MKT":
                        logger.debug("Running command api.place_order(buy_or_sell={}, product_type={}, exchange={}, " +
                                "tradingsymbol={}, quantity={}, discloseqty=0, price_type='MKT', price=0.0, " +
                                "retention='DAY', remarks='market_order')", orderType, productType, exchange, tradingsymbol, quantity);

                        res = this.api.place_order(orderType, productType, exchange, tradingsymbol, quantity, 0, "MKT", 0.0, "market_order", null, "DAY", null, null, null, null);

                        logger.info("Response: {}", res);
                        break;
                    case "SL-LMT":
                        logger.debug("Running command api.place_order(buy_or_sell={}, product_type={}, exchange={}, " +
                                "tradingsymbol={}, quantity={}, discloseqty=0, price_type='SL-LMT', price={}, " +
                                "trigger_price={}, retention='DAY', remarks='stop_loss_order')", orderType, productType, exchange, tradingsymbol, quantity, price, triggerPrice);

                        res = this.api.place_order(orderType, productType, exchange, tradingsymbol, quantity, 0, "SL-LMT", price, "stop loss order", triggerPrice, "DAY", null, null, null, null);

                        logger.info("Response: {}", res);
                        break;
                    case "LMT":
                        logger.debug("Running command api.place_order(buy_or_sell={}, product_type={}, exchange={}, " +
                                "tradingsymbol={}, quantity={}, discloseqty=0, price_type='LMT', price={}, " +
                                "retention='DAY', remarks='limit_order')", orderType, productType, exchange, tradingsymbol, quantity, price);

                        res = this.api.place_order(orderType, productType, exchange, tradingsymbol, quantity, 0, "LMT", price, "limit sell order", null, "DAY", null, null, null, null);

                        logger.info("Response: {}", res);
                        break;

                    default:
                        logger.error("Order type is not of defined ones");
                    }

                // Increment try counter and sleep between attempts
                currentTries++;
                logger.warn("trying to place order again");
                if (currentTries >= MAX_TRIES) {
                    logger.error("Max attempts to place order failed");
                    break;
                }
                Thread.sleep(2000);  // Sleep for 2 seconds
            }
        } catch (Exception e) {
            logger.error("Error in placing order: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        return res;
    }

    public  JSONObject cancelOrder(JSONObject order) {
        JSONObject res = new JSONObject();
        res.put("rejreason", true);
        int currentTries = 0;

        try {
            while (res.has("rejreason")) {
                String orderNumber = order.getString("norenordno"); // Assuming `Order` has a method `getNorenordno()`
                logger.debug("Cancelling order {}", order);
                res = api.cancel_order(orderNumber); // Assuming `api.cancelOrder()` returns a Map<String, Object>
                logger.info("Response: {}", res);

                currentTries++;

                if (currentTries >= MAX_TRIES) {
                    logger.info("Max attempts to cancel order failed");
                    break;
                }

                Thread.sleep(500);
            }
        } catch (Exception e) {
            logger.error("Error in cancelling order: {}", e.getMessage(), e);
        }

        return res;
    }


    public JSONArray getOrderBook() {
        int currentTries = 0;
        JSONArray orderBook = null;

        try {
            while (orderBook == null) {
                orderBook = this.api.get_order_book();
                currentTries++;
                TimeUnit.MILLISECONDS.sleep(100); // Wait 100ms between retries

                if (currentTries >= MAX_TRIES) {
                    logger.info("Max attempts to call order book API failed");
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error in getting order book: {}", e.getMessage(), e);
        }

        return orderBook;
    }

    public void exitAllPendingOrders() {
        JSONObject ret;
        JSONArray orderBook = getOrderBook();

        for (int i = 0; i < orderBook.length(); i++) {
            JSONObject order = orderBook.getJSONObject(i);
            if ("TRIGGER_PENDING".equals(order.getString("status")) || "OPEN".equals(order.getString("status"))) {
                if ("S".equals(order.getString("trantype"))) {
                    logger.debug("Converting all sell orders to market orders");
                    logger.debug("Running command ShoonyaHelper.modifyOrder(api, logger, {}, 'MKT')", order);
                    ret = modifyOrder(order.getString("exch"), order.getString("tsym"),
                            order.getString("norenordno") , order.getInt("qty"), "MKT", 0.0, null);
                    logger.debug("Modify Order Response: {}", ret);
                }

                if ("B".equals(order.getString("trantype"))) {
                    logger.debug("Cancelling all buy positions");
                    logger.debug("Running command ShoonyaHelper.cancelOrder(api, logger, {})", order);
                    ret = cancelOrder(order);
                    logger.debug("Cancel Order Response: {}", ret);
                }
            }
        }
    }

    public void exitAllOpenPositions() {
        JSONArray positions = this.api.get_positions();
        for (int j = 0; j < positions.length(); j++) {
            JSONObject position = positions.getJSONObject(j);
            int netQty = position.getInt("netqty");
            if (netQty < 0) {
                logger.debug("Closing short position {} with market order", position);
                placeOrder("B", position.getString("prd"), position.getString("exch"), position.getString("tsym"),
                        Math.abs(netQty), "MKT", 0, null);
            }

            if (netQty > 0) {
                logger.debug("Closing long position {} with market order", position);
                placeOrder("S", position.getString("prd"), position.getString("exch"), position.getString("tsym"),
                        netQty, "MKT", 0, null);
            }
        }
    }


    public void exitAllMarketOrders() {
        try {
            logger.debug("Exiting all positions via market order");
            exitAllPendingOrders();

            exitAllOpenPositions();
        }
        catch(Exception e){
            logger.error("couldnt not run market order exit with error {}", e.getMessage());
        }
    }




    public JSONObject withdraw(){
        JSONObject res = new JSONObject();
        try{
            float maxpay = this.api.get_max_payout_amount().getFloat("payout") -1;
            if(maxpay > 10) {
                logger.info("withdrawing money {}", maxpay);
                res = this.api.funds_payout("" + maxpay, "" + maxpay * -100);
                logger.info("funds withdrawn {}", res);
            }
            }
        catch(Exception  e){
            logger.error("error in withadrawing positions {}", e.getMessage());
        }
        return res;
    }


    public int requestFunds(Double funds){
        String withdraw =  "" + funds;
        int returnCode = api.collect_trans_req(withdraw);
        return returnCode;
    }

}