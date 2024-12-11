package com.example.trade_server.service;

import com.example.trade_server.lib.ShoonyaWebSocket;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import com.neovisionaries.ws.client.WebSocketException;
import com.noren.javaapi.NorenApiJava;
import org.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ShoonyaWebsocketService {

    private static final Logger logger = LogManager.getLogger(ShoonyaWebsocketService.class);
    public ShoonyaWebSocket client;
    public boolean feed_opened = false;
    private static final Map<String, Map<String, Object>> feedJson = new HashMap<>();
    private static final Map<String, Double> ltps = new HashMap<>();

    final static String  websocketEndpoint = "wss://api.shoonya.com/NorenWSTP/";


    public void event_handler_order_update(JSONObject orderUpdate){
        logger.debug("order feed {order_update}");
        try {
//        update_orders(order_update); TODO: add function in another service
        } catch (java.lang.Exception e) {
//            throw new RuntimeException(e);
            logger.error("update order error occured {}", e);
        }
    }


    public static void eventHandlerFeedUpdate(JSONObject tickData) {
        boolean UPDATE = false;

        if (tickData.has("tk")) {
            String token = tickData.getString("tk");
            LocalDateTime timest = LocalDateTime.ofInstant(Instant.ofEpochSecond(tickData.getLong("ft")), ZoneOffset.UTC);
            Map<String, Object> feedData = new HashMap<>();
            feedData.put("tt", timest.toString()); // ISO format

            if (tickData.has("lp")) {
                feedData.put("ltp", tickData.getDouble("lp"));
            }
            if (tickData.has("ts")) {
                feedData.put("Tsym", tickData.getString("ts"));
            }
            if (tickData.has("oi")) {
                feedData.put("openi", tickData.getDouble("oi"));
            }
            if (tickData.has("poi")) {
                feedData.put("pdopeni", tickData.getString("poi"));
            }
            if (tickData.has("v")) {
                feedData.put("Volume", tickData.getString("v"));
            }

            if (!feedData.isEmpty()) {
                UPDATE = true;
                feedJson.putIfAbsent(token, new HashMap<>());
                feedJson.get(token).putAll(feedData);
            }

            if (UPDATE) {
                if (feedData.containsKey("ltp")) {
                    try {
                        ltps.put(token, Double.parseDouble(feedJson.get(token).get("ltp").toString()));

                        // Call manage_option_sl if trade is active for token
//                        if (TRADE.containsKey(token)) {
//                            manageOptionSl(token, feedData);
//                          }
                    } catch (Exception e) {
                        logger.error("Error with feed occurred: {}", e.getMessage());
                    }
                }
            }
        }
    }

    public void open_callback(){
        this.feed_opened = true;
        logger.info("websocket opened");
    }

    public void startWebsocket(NorenApiJava api) throws Exception {

        ShoonyaWebSocket.WebSocketHandler handler = new ShoonyaWebSocket.WebSocketHandler() {
            @Override
            public void onTextMessage(String message) {
//                System.out.println("Message received: " + message);
                JSONObject res = new JSONObject(message);

                //feed update
                if (res.getString("t") .equals("tk") || res.getString("t") .equals("tf"))
                    eventHandlerFeedUpdate(res);
                if (res.getString("t") .equals("dk") || res.getString("t") .equals("df"))
                    eventHandlerFeedUpdate(res);

                // feed order update
                if (res.getString("t") .equals("om"))
                    event_handler_order_update(res);

                // feed started
                if (res.getString("t") .equals("ck") && res.getString("s") .equals("OK"))
                    open_callback();

                // feed error
                if (res.getString("t") .equals("ck") && !res.getString("s") .equals("OK"))
                    logger.error("Error with feed {}", res);
            }


            @Override
            public void onError(WebSocketException cause) {
                System.err.println("Error occurred: " + cause.getMessage());
            }
        };
        client = new ShoonyaWebSocket(websocketEndpoint, api, handler);
//        client = new ShoonyaWebSocketNeo(websocketEndpoint, api);

        client.connect();
        while (!this.feed_opened)
            TimeUnit.SECONDS.sleep(1);

    }

    public void subscribe(String exch, String token){
        String instrument = exch + "|" + token;
        this.client.subscribe(instrument, NorenApiJava.FeedType.TOUCHLINE);
        logger.info("subscribed to {}", instrument );
    }
}

