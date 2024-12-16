package com.shoonya.trade_server.service;

import com.shoonya.trade_server.config.ShoonyaConfig;
import com.shoonya.trade_server.entity.TokenInfo;
import com.shoonya.trade_server.lib.ShoonyaWebSocket;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import com.neovisionaries.ws.client.WebSocketException;
import com.noren.javaapi.NorenApiJava;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@Service
public class ShoonyaWebsocketService {

    ShoonyaConfig shoonyaConfig;

    OrderManagementService orderManagementService;

    NorenApiJava api;
    public ShoonyaWebsocketService(ShoonyaLoginService shoonyaLoginService,  ShoonyaConfig shoonyaConfig, OrderManagementService orderManagementService){
        this.api = shoonyaLoginService.getApi();
        this.shoonyaConfig = shoonyaConfig;
        this.orderManagementService = orderManagementService;
    }

    private static final Logger logger = LogManager.getLogger(ShoonyaWebsocketService.class);
    private ShoonyaWebSocket wsClient;
    private boolean feedOpened = false;
    private  final Map<String, Map<String, Object>> feedJson = new HashMap<>();
    private  final Map<String, Double> ltps = new HashMap<>();




    public void eventHandlerOrderUpdate(JSONObject orderUpdate){
        logger.info("order feed {}", orderUpdate);
        try {
        orderManagementService.updateOrder(wsClient, orderUpdate);
        } catch (java.lang.Exception e) {
            logger.error("update order error occured {}", e.getMessage());
            // Log with the specific line number
            StackTraceElement element = e.getStackTrace()[0];
            logger.error("Error occurred at line: {}", element.getLineNumber());
        }
    }


    public void eventHandlerFeedUpdate(JSONObject tickData) {
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

                        orderManagementService.manageOptionSl(token, ltps.get(token));
                    } catch (Exception e) {
                        logger.error("Error with feed occurred: {}", e.getMessage());
                    }
                }
            }
        }
    }

    public void openCallback(){
        this.feedOpened = true;
        logger.info("websocket opened");
    }

    @PostConstruct
    public void startWebsocket() throws Exception {

        String websocketEndpoint = shoonyaConfig.getWebsocket();
        ShoonyaWebSocket.WebSocketHandler handler = new ShoonyaWebSocket.WebSocketHandler() {
            @Override
            public void onTextMessage(String message) {
//                logger.info("Message received: {}" , message);
                JSONObject res = new JSONObject(message);

                //feed update
                if (res.getString("t") .equals("tk") || res.getString("t") .equals("tf"))
                    eventHandlerFeedUpdate(res);
                if (res.getString("t") .equals("dk") || res.getString("t") .equals("df"))
                    eventHandlerFeedUpdate(res);

                // feed order update
                if (res.getString("t") .equals("om"))
                    eventHandlerOrderUpdate(res);

                // feed started
                if (res.getString("t") .equals("ck") && res.getString("s") .equals("OK"))
                    openCallback();

                // feed error
                if (res.getString("t") .equals("ck") && !res.getString("s") .equals("OK"))
                    logger.error("Error with feed {}", res);
                    // Log with the specific line number
            }


            @Override
            public void onError(WebSocketException cause) {
                logger.error("Error occurred: {}" , cause.getMessage());
            }
        };
        this.wsClient = new ShoonyaWebSocket(websocketEndpoint, this.api, handler);
//        client = new ShoonyaWebSocketNeo(websocketEndpoint, api);

        this.wsClient.connect();
        //TODO: use asynchronous? what is the use
        while (!this.feedOpened)
            TimeUnit.SECONDS.sleep(1);

    }

    public void subscribe(TokenInfo tokenInfo){
        String instrument = tokenInfo.getInstrument();
        this.wsClient.subscribe(instrument, NorenApiJava.FeedType.TOUCHLINE);
        logger.info("subscribed to {}", instrument );
    }

    public void unsubscribe(TokenInfo tokenInfo){
        String instrument = tokenInfo.getInstrument();
        this.wsClient.unsubscribe(instrument, NorenApiJava.FeedType.TOUCHLINE);
        logger.info("unsubscribed from {}", instrument );
    }
}

