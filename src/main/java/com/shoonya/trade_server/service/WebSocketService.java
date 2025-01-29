package com.shoonya.trade_server.service;

import com.shoonya.trade_server.handler.WebSocketHandler;
import com.shoonya.trade_server.lib.Misc;
import com.shoonya.trade_server.lib.ShoonyaHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;


import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@Service
class MessageQueue {

    private final Queue<String> queue = new LinkedList<>();

    public void addMessage(String message) {
        queue.add(message);
    }

    public String getNextMessage() {
        return queue.poll();
    }

    public boolean isQueueEmpty() {
        return queue.isEmpty();
    }
}

@Service
public class WebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);
    private final WebSocketHandler webSocketHandler;
    private final Misc misc;
    private  WebSocketSession session;
    public WebSocketService(WebSocketHandler webSocketHandler, Misc misc) {
        this.webSocketHandler = webSocketHandler;
        this.misc = misc;
        this.session = getWebsocketSession();
    }

    public WebSocketSession getWebsocketSession(){
//        WebSocketSession session =  null;
//        while(session == null){
//            try {
//                session =  this.webSocketHandler.getSession();
//                logger.info("websocket session not active yet, sleeping for a second");
//                TimeUnit.SECONDS.sleep(10);
//            } catch (Exception e) {
//            logger.error("Failed to wait for websocket session: {}", e.getMessage());
//            }
//        }
//        return session;
        return this.webSocketHandler.getSession();
    }
//
////    private MessageQueue queue = new MessageQueue();
    private final Queue<String> queue = new ConcurrentLinkedQueue<>();

    public synchronized void sendMessage(String message) {
        try {
            queue.add(message);
            processQueue();
        } catch (Exception e) {
            logger.error("Failed to queue message: {}", e.getMessage());
        }
    }

    private synchronized void processQueue() {
        while (!queue.isEmpty()) {
            session =  this.webSocketHandler.getSession();
            if (session != null && session.isOpen()) {
                try {
                    String nextMessage = queue.poll();
                    if (nextMessage != null) {
                        session.sendMessage(new TextMessage(nextMessage));
                        logger.debug("Sent message: {}", nextMessage);
                    }
                } catch (IllegalStateException e) {
                    logger.warn("WebSocket session is in an invalid state: {}", e.getMessage());
                } catch (Exception e) {
                    logger.error("Failed to send message: {}", e.getMessage());
                }
            } else {
                logger.debug("WebSocket session is not open");
//                Thread.sleep(1000);
                break;
            }
        }
    }


//    public void sendMessage(String message) {
//        boolean lock = webSocketHandler.isLock();
//        if(lock){
//            logger.info("websocket closed, waiting for it");
//            try {
//                Thread.sleep(1000);
//                lock = webSocketHandler.isLock();
//            }
//            catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        final int maxRetries = 5; // Maximum number of retries
//        final long retryDelay = 1000; // Delay between retries in milliseconds
//        for (int attempt = 1; attempt <= maxRetries; attempt++) {
//            try {
//                WebSocketSession session = webSocketHandler.getSession();
//                if (session != null && session.isOpen()) {
//                    session.sendMessage(new TextMessage(message));
////                    logger.info("Sent message: {}", message);
//                    return; // Exit the method if message is sent successfully
//                } else {
//                    logger.warn("WebSocket session is not open");
//                }
//            } catch (IllegalStateException e) {
//                logger.warn("WebSocket session is in an invalid state: {}", e.getMessage());
//            } catch (Exception e) {
//                logger.error("Failed to send message: {}", e.getMessage());
//            }
//
//            // Wait before retrying
//            try {
//                Thread.sleep(retryDelay);
//            } catch (InterruptedException e) {
//                logger.error("Retry was interrupted: {}", e.getMessage());
//                Thread.currentThread().interrupt();
//            }
//        }
//    }

    public void sendToast(String title, String description){
        JSONObject res  =  new JSONObject();
        res.put("type", "toast");
        res.put("title", title);
        res.put("description", description);
        sendMessage(res.toString());
    }

    public void sendPriceFeed(String token, Long epoch, double price){
        JSONObject res  =  new JSONObject();
        res.put("type", "price");
        res.put("token", token);
        res.put("tt", epoch);
        res.put("price", price);
        sendMessage(res.toString());
    }

    public void updateAtmOptions(String ceToken, String ceTsym, String peToken, String peTsym){
        JSONObject res  =  new JSONObject();
        res.put("type", "atm");
        res.put("ceToken", ceToken);
        res.put("peToken", peToken);
        res.put("ceTsym", ceTsym);
        res.put("peTsym", peTsym);
        sendMessage(res.toString());
    }

    public void updateOrderFeed(JSONArray orders){
        JSONObject res  =  new JSONObject();
        res.put("type", "order");
        res.put("orders", orders);
        String message = res.toString();
        sendMessage(res.toString());
    }

    public void updatePositionFeed(JSONArray positions){
        JSONObject res  =  new JSONObject();
        res.put("type", "position");
        res.put("positions", positions);
        String message = res.toString();
        sendMessage(res.toString());
    }

    public void updateTimer(String timer){
        JSONObject res  =  new JSONObject();
        res.put("type", "timer");
        res.put("left", timer);
        String message = res.toString();
        sendMessage(res.toString());
    }

}